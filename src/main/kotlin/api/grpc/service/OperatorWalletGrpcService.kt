package api.grpc.service

import application.Bus
import application.command.session.EndCasinoRoundSessionCommand
import application.command.session.PlaceSpinCasinoSessionCommand
import application.command.session.ReopenCasinoRoundSessionCommand
import application.command.session.SettleSpinCasinoSessionCommand
import application.query.session.FindCasinoSessionBalanceQuery
import application.query.session.FindCasinoSessionQuery
import domain.exception.badrequest.BetCurrencyMismatchException
import domain.exception.badrequest.BlankCurrencyException
import domain.exception.forbidden.InsufficientBalanceException
import domain.exception.forbidden.MaxPlaceSpinException
import domain.exception.notfound.CasinoSessionNotFoundException
import domain.model.CasinoSession
import domain.model.PlayerBalance
import domain.vo.Amount
import domain.vo.Currency
import gamehub.v1.Common
import gamehub.v1.Operator
import gamehub.v1.OperatorWalletServiceGrpcKt
import gamehub.v1.balanceResult
import gamehub.v1.spinResult
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("api.grpc.OperatorWallet")

private val OPERATOR_ID_HEADER: Metadata.Key<String> =
    Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER)

private val OPERATOR_KEY_HEADER: Metadata.Key<String> =
    Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER)

private val ERROR_CODE_KEY: Metadata.Key<String> =
    Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

private val OPERATOR_CREDENTIALS: Context.Key<OperatorCredentials> =
    Context.key("gamehub-operator-credentials")

/** What the hub sends us on every call: its copy of our operator identity and secret. */
data class OperatorCredentials(val id: String, val key: String)

/**
 * Lifts `x-operator-id` / `x-operator-key` off the wire into the gRPC [Context], which grpc-kotlin
 * carries into the coroutine running the call — a `CoroutineImplBase` method never sees the
 * request headers itself. Only headers, no verification: the secret to compare against is reached
 * through the session named by the request body, which this layer has not read yet.
 */
object OperatorCredentialsInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val id = headers.get(OPERATOR_ID_HEADER)
        val key = headers.get(OPERATOR_KEY_HEADER)

        val context =
            if (id.isNullOrBlank() || key.isNullOrBlank()) Context.current()
            else Context.current().withValue(OPERATOR_CREDENTIALS, OperatorCredentials(id, key))

        return Contexts.interceptCall(context, call, headers, next)
    }
}

/**
 * `gamehub.v1.OperatorWalletService` — the INBOUND half of the GameHub integration: the hub calls
 * us to move money in prematch's wallet. Registered behind [OperatorCredentialsInterceptor].
 *
 * **Direction is the sign.** The contract carries no operation type: `real_amount + bonus_amount`
 * negative debits the player, positive credits them. See [spin] for what that costs.
 *
 * **Idempotency** is keyed on `SpinRequest.id`, which the hub guarantees unique per leg. It lands
 * as `ExternalSpinId`, and the place/settle handlers already answer a replay with the balance the
 * first call produced without booking a second spin or a second wallet move — so this service adds
 * no replay logic of its own.
 *
 * **Money** is int64 nano on both sides, the fleet wallet unit, so nothing is converted here.
 *
 * Authentication is the aggregator config of the session being played, under the keys
 * [CONFIG_OPERATOR_ID] and [CONFIG_OPERATOR_KEY] — the same pair we send outbound to the hub.
 */
class OperatorWalletGrpcService(
    private val bus: Bus,
) : OperatorWalletServiceGrpcKt.OperatorWalletServiceCoroutineImplBase() {

    override suspend fun spin(request: Operator.SpinRequest): Operator.SpinResult = handleOperatorCall {
        val session = authorizedSession(
            sessionRef = if (request.hasSessionRef()) request.sessionRef else null,
            playerId = request.playerId,
            currency = request.currency,
        )

        // Route by sign — there is no type field to route by.
        //
        // The consequence, and it is deliberate: a ROLLBACK is indistinguishable here. The hub
        // sends the compensating amount under a NEW id, so refunding a bet reaches us positive and
        // is booked as a SETTLE, and reclaiming a win reaches us negative and is booked as a PLACE.
        // That means the daily RTP job counts a reversal as an ordinary spin. Do not try to detect
        // rollbacks: the only available signal would be guessing from amounts, which is wrong more
        // often than it is right, and a wrong guess books real money to the wrong side.
        val total = request.realAmount + request.bonusAmount
        val freespinId = if (request.hasFreespinId()) request.freespinId.takeIf { it.isNotBlank() } else null

        // A correcting movement legitimately arrives after its round closed — the hub sends a
        // rollback as the compensating amount under a new id, and a vendor can give one up hours
        // later. `SpinFactory` refuses a place or a settle on a finished round, which is right for
        // a vendor inventing new play and wrong for money that has to come back, so the round is
        // reopened first. A round that is still open is untouched.
        if (total != 0L) {
            try {
                bus(ReopenCasinoRoundSessionCommand(session = session, externalRoundId = request.roundId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The usual case: the first leg of a round, so there is nothing to reopen yet.
                logger.debug("No round to reopen for {}: {}", request.roundId, e.message)
            }
        }

        val playerBalance = when {
            total < 0 -> bus(
                PlaceSpinCasinoSessionCommand(
                    session = session,
                    externalRoundId = request.roundId,
                    externalSpinId = request.id,
                    freespinId = freespinId,
                    amount = Amount(-total),
                )
            )

            total > 0 -> bus(
                SettleSpinCasinoSessionCommand(
                    session = session,
                    externalRoundId = request.roundId,
                    externalSpinId = request.id,
                    freespinId = freespinId,
                    amount = Amount(total),
                )
            )

            // A zero leg moves nothing — a vendor's no-op, or a free round an operator pays
            // nothing for. Booking an empty spin would put a row and an event behind a movement
            // that never happened, so answer with what the player actually holds.
            else -> bus(FindCasinoSessionBalanceQuery(session))
        }

        if (request.roundClose) closeRound(session, request.roundId)

        spinResult {
            // We echo the hub's own id. casino-engine keys the spin by exactly this string
            // (`ExternalSpinId`), so it already IS our reference for the movement — there is no
            // second, engine-side handle to hand back, and echoing it keeps the hub's
            // reconciliation a direct join instead of a mapping table.
            id = request.id
            balance = toProto(playerBalance)
        }
    }

    override suspend fun getBalance(request: Operator.BalanceRequest): Operator.BalanceResult = handleOperatorCall {
        val session = authorizedSession(
            sessionRef = if (request.hasSessionRef()) request.sessionRef else null,
            playerId = request.playerId,
            currency = request.currency,
        )

        val playerBalance = bus(FindCasinoSessionBalanceQuery(session))

        balanceResult {
            balance = toProto(playerBalance)
        }
    }

    /**
     * Resolves the session the money belongs to, proves the caller may move it, and pins the
     * request currency onto it.
     *
     * `session_ref` is the `CasinoSession.token` we handed the hub on `OpenGame`, so it names one
     * exact session. Without it there is nothing to resolve: `(player, game)` stops picking a
     * single session the moment a player opens the same game in two tabs, and attaching the money
     * to the wrong round is worse than refusing it. An operator that does not pass `session_ref`
     * cannot be served.
     */
    private suspend fun authorizedSession(sessionRef: String?, playerId: String, currency: String): CasinoSession {
        val credentials = OPERATOR_CREDENTIALS.get()
            ?: throw operatorError(Status.UNAUTHENTICATED, ERROR_OPERATOR_UNAUTHENTICATED, "Operator credentials missing")

        if (sessionRef.isNullOrBlank()) {
            throw operatorError(Status.NOT_FOUND, ERROR_PLAYER_NOT_FOUND, "session_ref is required")
        }

        val session = bus(FindCasinoSessionQuery(sessionRef))

        session.verifyCredentials(credentials)

        // The body is not evidence. A ref that resolves to a different player is either a hub bug
        // or a forged callback, and either way the money must not move.
        if (session.playerId.value != playerId) {
            throw operatorError(Status.PERMISSION_DENIED, ERROR_PLAYER_BLOCKED, "Player does not own this session")
        }

        // Pin the request currency the way the TONGame webhook does: a call carrying a currency
        // the session did not open in is still booked consistently, against the wallet the hub
        // named rather than the one we remembered.
        return session.copy(currency = Currency(currency))
    }

    /**
     * Compares the caller's credentials against the config of the aggregator serving THIS session,
     * never against a hardcoded aggregator name. A session opened through some other aggregator
     * carries no operator credentials in its config, so it authenticates nobody — which is the
     * point: the hub can only move money in sessions the hub itself opened.
     */
    private fun CasinoSession.verifyCredentials(credentials: OperatorCredentials) {
        val config = gameVariant.game.provider.aggregator.config
        val expectedId = config[CONFIG_OPERATOR_ID]?.toString().orEmpty()
        val expectedKey = config[CONFIG_OPERATOR_KEY]?.toString().orEmpty()

        val accepted = expectedId.isNotBlank() &&
            expectedKey.isNotBlank() &&
            constantTimeEquals(expectedId, credentials.id) &&
            constantTimeEquals(expectedKey, credentials.key)

        if (!accepted) {
            throw operatorError(Status.UNAUTHENTICATED, ERROR_OPERATOR_UNAUTHENTICATED, "Invalid operator credentials")
        }
    }

    /**
     * Closes the round after the money has landed. Bookkeeping must never sink a movement that is
     * already committed: an unclosed round costs a report line, a spin refused because its round
     * would not close costs the player the bet.
     */
    private suspend fun closeRound(session: CasinoSession, externalRoundId: String) {
        try {
            bus(EndCasinoRoundSessionCommand(session = session, externalRoundId = externalRoundId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(
                "Round close failed after the movement landed [session={}, round={}]: {}",
                session.token.value,
                externalRoundId,
                e.message,
            )
        }
    }

    private fun toProto(balance: PlayerBalance): Common.Balance = Common.Balance.newBuilder()
        .setReal(balance.realAmount.value)
        .setBonus(balance.bonusAmount.value)
        .setCurrency(balance.currency.value)
        .build()

    private companion object {

        /** Aggregator config entries holding our identity and secret at the hub. */
        const val CONFIG_OPERATOR_ID = "operatorId"

        const val CONFIG_OPERATOR_KEY = "operatorKey"
    }
}

/**
 * Maps what casino-engine throws onto the codes the hub reads (`docs/ERRORS.md`, section
 * "Оператор → хаб"), attaching the machine-readable reason as `x-error-code`.
 *
 * Nothing here answers `UNAVAILABLE`: the hub retries a spin exactly once on it, and every
 * condition below is a settled answer that a second identical call would reach again.
 */
private suspend fun <T> handleOperatorCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: StatusException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: InsufficientBalanceException) {
        throw operatorError(Status.FAILED_PRECONDITION, ERROR_INSUFFICIENT_FUNDS, e.message)
    } catch (e: MaxPlaceSpinException) {
        throw operatorError(Status.FAILED_PRECONDITION, ERROR_BET_LIMIT_EXCEEDED, e.message)
    } catch (e: CasinoSessionNotFoundException) {
        throw operatorError(Status.NOT_FOUND, ERROR_PLAYER_NOT_FOUND, e.message)
    } catch (e: BetCurrencyMismatchException) {
        throw operatorError(Status.INVALID_ARGUMENT, ERROR_CURRENCY_MISMATCH, e.message)
    } catch (e: BlankCurrencyException) {
        // The catalogue the hub reads has no generic validation code for a wallet call, and a
        // missing currency is a currency we cannot match against the player's wallet.
        throw operatorError(Status.INVALID_ARGUMENT, ERROR_CURRENCY_MISMATCH, e.message)
    } catch (e: Exception) {
        logger.error("Operator wallet call failed", e)
        throw operatorError(Status.INTERNAL, ERROR_INTERNAL, "Internal error")
    }

private fun operatorError(status: Status, code: String, description: String?): StatusException {
    val metadata = Metadata()
    metadata.put(ERROR_CODE_KEY, code)
    return StatusException(status.withDescription(description), metadata)
}

/** Length-safe, timing-safe secret comparison. */
private fun constantTimeEquals(expected: String, actual: String): Boolean =
    MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))

private const val ERROR_OPERATOR_UNAUTHENTICATED = "OPERATOR_UNAUTHENTICATED"

private const val ERROR_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"

private const val ERROR_BET_LIMIT_EXCEEDED = "BET_LIMIT_EXCEEDED"

private const val ERROR_PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND"

private const val ERROR_PLAYER_BLOCKED = "PLAYER_BLOCKED"

private const val ERROR_CURRENCY_MISMATCH = "CURRENCY_MISMATCH"

private const val ERROR_INTERNAL = "INTERNAL"
