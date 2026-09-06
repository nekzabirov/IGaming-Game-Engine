package grpc

import GameHubConfig
import clients.Balance
import errors.CasinoRoundNotFoundException
import errors.InsufficientBalanceException
import errors.MaxPlaceSpinException
import errors.Valid
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
import org.slf4j.LoggerFactory
import services.WalletService
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

private val log = LoggerFactory.getLogger("grpc.Wallet")

private val OPERATOR_ID_HEADER: Metadata.Key<String> = Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER)

private val OPERATOR_KEY_HEADER: Metadata.Key<String> = Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER)

private val ERROR_CODE_KEY: Metadata.Key<String> = Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

private val HUB_CREDENTIALS: Context.Key<HubCredentials> = Context.key("gamehub-credentials")

/** What the hub sends on every call: its copy of our identity and secret. */
data class HubCredentials(val id: String, val key: String)

/** Lifts the hub's credential headers into the gRPC Context — a coroutine service never sees headers itself. */
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
 * `gamehub.v1.WebhookService` — the hub calls us for every money movement, naming
 * `player_id`+`game`+`round_id` directly. Authenticated against the SAME operator pair we send
 * outbound; a refusal names its reason in `x-error-code`. `INTERNAL` is the one code part of the
 * vendor fleet reads as "start a rollback cycle" — used only when the outcome is truly unknown.
 */
class WalletGrpcService(
    private val wallet: WalletService,
    private val config: GameHubConfig,
) : WebhookServiceGrpcKt.WebhookServiceCoroutineImplBase() {

    override suspend fun balance(request: Webhook.BalanceRequest): Webhook.BalanceResponse = handleHubCall {
        authenticate()
        val balance = wallet.balance(Valid.playerId(request.playerId), Valid.currency(request.currency))
        balanceResponse { this.balance = balance.toProto() }
    }

    override suspend fun placeSpin(request: Webhook.PlaceSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()
        val balance = wallet.place(
            WalletService.Leg(
                id = Valid.externalId(request.id),
                roundId = Valid.externalId(request.roundId),
                playerId = Valid.playerId(request.playerId),
                game = request.game.takeIf { it.isNotBlank() }?.let { Valid.identity(it) },
                amount = Valid.amount(request.amount),
                currency = Valid.currency(request.currency),
                freespinId = if (request.hasFreespinId()) Valid.freespinId(request.freespinId) else null,
            ),
        )
        spinResponse {
            // Our leg id IS the hub's — echoing it keeps the hub's reconciliation a direct join.
            id = request.id
            this.balance = balance.toProto()
        }
    }

    override suspend fun settleSpin(request: Webhook.SettleSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()
        val balance = wallet.settle(
            WalletService.Leg(
                id = Valid.externalId(request.id),
                roundId = Valid.externalId(request.roundId),
                playerId = Valid.playerId(request.playerId),
                game = request.game.takeIf { it.isNotBlank() }?.let { Valid.identity(it) },
                amount = Valid.amount(request.amount),
                currency = Valid.currency(request.currency),
                freespinId = if (request.hasFreespinId()) Valid.freespinId(request.freespinId) else null,
            ),
        )
        spinResponse {
            id = request.id
            this.balance = balance.toProto()
        }
    }

    override suspend fun rollbackSpin(request: Webhook.RollbackSpinRequest): Webhook.SpinResponse = handleHubCall {
        authenticate()
        val reversed = wallet.rollback(Valid.externalId(request.id))
        spinResponse {
            id = request.id
            // Null means there was no such leg: success, and no player to read a balance for.
            if (reversed != null) this.balance = reversed.toProto()
        }
    }

    override suspend fun closeRound(request: Webhook.CloseRoundRequest): Webhook.CloseRoundResponse = handleHubCall {
        authenticate()
        // The money has already landed — a round that cannot be closed must not fail the call, or
        // the hub would treat a movement that went through as refused and retry it.
        try {
            wallet.closeRound(request.roundId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Round close failed after the movement landed [round={}]: {}", request.roundId, e.message)
        }
        Webhook.CloseRoundResponse.getDefaultInstance()
    }

    private fun authenticate() {
        val credentials = HUB_CREDENTIALS.get()
            ?: throw hubError(Status.UNAUTHENTICATED, ERROR_PLAYER_NOT_FOUND, "Hub credentials missing")

        val accepted = constantTimeEquals(config.operatorId, credentials.id) &&
            constantTimeEquals(config.operatorKey, credentials.key)

        if (!accepted) throw hubError(Status.UNAUTHENTICATED, ERROR_PLAYER_NOT_FOUND, "Invalid hub credentials")
    }

    private fun Balance.toProto(): Webhook.Balance = Webhook.Balance.newBuilder()
        .setReal(real)
        .setBonus(bonus)
        .setCurrency(currency)
        .build()
}

/** Maps what casino-engine throws onto the codes the hub reads (docs/ERRORS.md in the hub repo). */
private suspend fun <T> handleHubCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: HubRefusal) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: InsufficientBalanceException) {
        throw hubError(Status.FAILED_PRECONDITION, ERROR_INSUFFICIENT_FUNDS, e.message)
    } catch (e: MaxPlaceSpinException) {
        throw hubError(Status.FAILED_PRECONDITION, ERROR_LIMIT_EXCEEDED, e.message)
    } catch (e: CasinoRoundNotFoundException) {
        throw hubError(Status.NOT_FOUND, ERROR_PLAYER_NOT_FOUND, e.message)
    } catch (e: Exception) {
        log.error("Hub wallet call failed", e)
        throw hubError(Status.INTERNAL, ERROR_INTERNAL, "Internal error")
    }

/** A refusal WE produced, with its `x-error-code` — the only StatusException allowed to pass through. */
private class HubRefusal(status: Status, trailers: Metadata) : StatusException(status, trailers)

private fun hubError(status: Status, code: String, description: String?): StatusException {
    val metadata = Metadata()
    metadata.put(ERROR_CODE_KEY, code)
    return HubRefusal(status.withDescription(description), metadata)
}

/** Length-safe, timing-safe secret comparison. */
private fun constantTimeEquals(expected: String, actual: String): Boolean =
    MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))

private const val ERROR_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"

private const val ERROR_LIMIT_EXCEEDED = "LIMIT_EXCEEDED"

private const val ERROR_PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND"

private const val ERROR_INTERNAL = "INTERNAL"
