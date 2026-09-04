package api.grpc.service

import application.Bus
import application.command.round.CloseRoundCommand
import application.command.round.PlaceSpinCommand
import application.command.round.ReopenRoundCommand
import application.command.round.RollbackSpinCommand
import application.command.round.SettleSpinCommand
import application.query.round.FindBalanceQuery
import domain.exception.notfound.CasinoRoundNotFoundException
import domain.exception.notfound.SpinNotFoundException
import domain.exception.forbidden.InsufficientBalanceException
import domain.exception.forbidden.MaxPlaceSpinException
import domain.model.PlayerBalance
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import gamehub.v1.Webhook
import gamehub.v1.WebhookServiceGrpcKt
import gamehub.v1.balanceResponse
import gamehub.v1.spinResponse
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import infrastructure.gamehub.GameHubConfig
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("api.grpc.Wallet")

private val OPERATOR_ID_HEADER: Metadata.Key<String> =
    Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER)

private val OPERATOR_KEY_HEADER: Metadata.Key<String> =
    Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER)

private val ERROR_CODE_KEY: Metadata.Key<String> =
    Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

private val HUB_CREDENTIALS: Context.Key<HubCredentials> = Context.key("gamehub-credentials")

/** What the hub sends us on every call: its copy of our identity and secret. */
data class HubCredentials(val id: String, val key: String)

/**
 * Lifts `x-operator-id`/`x-operator-key` off the wire into the gRPC [Context] — grpc-kotlin
 * carries the context into the coroutine running the call, but a `CoroutineImplBase` method never
 * sees the request headers directly.
 */
object HubCredentialsInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val id = headers.get(OPERATOR_ID_HEADER)
        val key = headers.get(OPERATOR_KEY_HEADER)

        val context =
            if (id.isNullOrBlank() || key.isNullOrBlank()) Context.current()
            else Context.current().withValue(HUB_CREDENTIALS, HubCredentials(id, key))

        return Contexts.interceptCall(context, call, headers, next)
    }
}

/**
 * `gamehub.v1.WebhookService` — the hub calls US now, directly, with no session in between:
 * every call names `player_id`+`game`+`round_id` on the wire. Registered behind
 * [HubCredentialsInterceptor], checked against the SAME [GameHubConfig] pair casino-engine sends
 * outbound to `GatewayService` — the check is symmetric, exactly as the hub's own contract expects.
 *
 * `game` empty means a sportsbook leg — see [PlaceSpinCommand]/[SettleSpinCommand].
 */
class WalletGrpcService(
    private val bus: Bus,
    private val config: GameHubConfig,
) : WebhookServiceGrpcKt.WebhookServiceCoroutineImplBase() {

    override suspend fun balance(request: Webhook.BalanceRequest): Webhook.BalanceResponse = handleHubCall {
        authenticate()

        val playerBalance = bus(
            FindBalanceQuery(
                playerId = PlayerId(request.playerId),
                currency = Currency(request.currency),
            )
        )

        balanceResponse { this.balance = toProto(playerBalance) }
    }

    override suspend fun placeSpin(request: Webhook.PlaceSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()
        reopenIfNeeded(request.roundId)

        val balance = bus(
            PlaceSpinCommand(
                externalSpinId = request.id,
                externalRoundId = request.roundId,
                playerId = PlayerId(request.playerId),
                gameIdentity = request.game.takeIf { it.isNotBlank() }?.let { Identity(it) },
                amount = Amount(request.amount),
                currency = Currency(request.currency),
                freespinId = if (request.hasFreespinId()) request.freespinId else null,
            )
        )

        spinResponse {
            // Our own leg id IS `request.id` — there is no second, engine-side handle to hand
            // back, and echoing it keeps the hub's reconciliation a direct join.
            id = request.id
            this.balance = toProto(balance)
        }
    }

    override suspend fun settleSpin(request: Webhook.SettleSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()
        reopenIfNeeded(request.roundId)

        val balance = bus(
            SettleSpinCommand(
                externalSpinId = request.id,
                externalRoundId = request.roundId,
                playerId = PlayerId(request.playerId),
                gameIdentity = request.game.takeIf { it.isNotBlank() }?.let { Identity(it) },
                amount = Amount(request.amount),
                currency = Currency(request.currency),
                freespinId = if (request.hasFreespinId()) request.freespinId else null,
            )
        )

        spinResponse {
            id = request.id
            this.balance = toProto(balance)
        }
    }

    override suspend fun rollbackSpin(request: Webhook.RollbackSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()

        val balance = bus(RollbackSpinCommand(externalSpinId = request.id))

        spinResponse {
            id = request.id
            this.balance = toProto(balance)
        }
    }

    override suspend fun closeRound(request: Webhook.CloseRoundRequest): Webhook.CloseRoundResponse = handleHubCall {
        authenticate()

        // The money has already landed by this point — a round that cannot be closed must not
        // fail the call, or the hub would treat a movement that legitimately went through as
        // refused and retry it.
        try {
            bus(CloseRoundCommand(externalRoundId = request.roundId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Round close failed after the movement landed [round={}]: {}", request.roundId, e.message)
        }

        Webhook.CloseRoundResponse.getDefaultInstance()
    }

    /**
     * Reopens a round for a correcting movement, before that movement is applied.
     *
     * Not part of `WebhookService` itself — [PlaceSpinCommand]/[SettleSpinCommand]'s handlers call
     * [ReopenRoundCommand] internally when a round could be finished, so a late rollback lands
     * cleanly. Kept here as the one place that knows both halves of that dance.
     */
    private suspend fun reopenIfNeeded(externalRoundId: String) {
        try {
            bus(ReopenRoundCommand(externalRoundId = externalRoundId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.debug("No round to reopen for {}: {}", externalRoundId, e.message)
        }
    }

    private fun authenticate() {
        val credentials = HUB_CREDENTIALS.get()
            ?: throw hubError(Status.UNAUTHENTICATED, ERROR_PLAYER_NOT_FOUND, "Hub credentials missing")

        val accepted = constantTimeEquals(config.operatorId, credentials.id) &&
            constantTimeEquals(config.operatorKey, credentials.key)

        if (!accepted) {
            throw hubError(Status.UNAUTHENTICATED, ERROR_PLAYER_NOT_FOUND, "Invalid hub credentials")
        }
    }

    private fun toProto(balance: PlayerBalance): Webhook.Balance = Webhook.Balance.newBuilder()
        .setReal(balance.realAmount.value)
        .setBonus(balance.bonusAmount.value)
        .setCurrency(balance.currency.value)
        .build()
}

/**
 * Maps what casino-engine throws onto the codes the hub reads (`docs/ERRORS.md` in the hub repo,
 * "Чем отвечает ОПЕРАТОР"), attaching the machine-readable reason as `x-error-code`.
 *
 * `INTERNAL` is the one code a part of the vendor fleet reads as "start a rollback cycle" — it is
 * used only where we genuinely do not know whether the movement landed, never for a settled
 * refusal.
 */
private suspend fun <T> handleHubCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: StatusException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: InsufficientBalanceException) {
        throw hubError(Status.FAILED_PRECONDITION, ERROR_INSUFFICIENT_FUNDS, e.message)
    } catch (e: MaxPlaceSpinException) {
        throw hubError(Status.FAILED_PRECONDITION, ERROR_LIMIT_EXCEEDED, e.message)
    } catch (e: CasinoRoundNotFoundException) {
        throw hubError(Status.NOT_FOUND, ERROR_PLAYER_NOT_FOUND, e.message)
    } catch (e: SpinNotFoundException) {
        throw hubError(Status.NOT_FOUND, ERROR_PLAYER_NOT_FOUND, e.message)
    } catch (e: Exception) {
        logger.error("Hub wallet call failed", e)
        throw hubError(Status.INTERNAL, ERROR_INTERNAL, "Internal error")
    }

private fun hubError(status: Status, code: String, description: String?): StatusException {
    val metadata = Metadata()
    metadata.put(ERROR_CODE_KEY, code)
    return StatusException(status.withDescription(description), metadata)
}

/** Length-safe, timing-safe secret comparison. */
private fun constantTimeEquals(expected: String, actual: String): Boolean =
    MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))

private const val ERROR_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"

private const val ERROR_LIMIT_EXCEEDED = "LIMIT_EXCEEDED"

private const val ERROR_PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND"

private const val ERROR_INTERNAL = "INTERNAL"
