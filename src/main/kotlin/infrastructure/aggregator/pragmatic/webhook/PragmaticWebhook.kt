package infrastructure.aggregator.pragmatic.webhook

import application.cqrs.Bus
import application.cqrs.session.EndRoundSessionCommand
import application.cqrs.session.FindSessionBalanceQuery
import application.cqrs.session.PlaceSpinSessionCommand
import application.cqrs.session.SettleSpinSessionCommand
import domain.exception.forbidden.InsufficientBalanceException
import domain.exception.forbidden.MaxPlaceSpinException
import domain.exception.notfound.SessionNotFoundException
import domain.model.PlayerBalance
import domain.vo.Amount
import infrastructure.aggregator.pragmatic.webhook.dto.PragmaticBetDto
import infrastructure.aggregator.pragmatic.webhook.dto.PragmaticResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.math.BigDecimal
import java.math.RoundingMode

class PragmaticWebhook(private val bus: Bus) {

    fun Route.route() = route("/pragmatic") {
        get("/authenticate.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            call.respond(authenticate(sessionToken))
        }

        get("/balance.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            call.respond(balance(sessionToken))
        }

        get("/bet.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            val payload = PragmaticBetDto(
                reference = call.parameters["reference"] ?: "",
                gameId = call.parameters["gameId"] ?: "",
                roundId = call.parameters["roundId"] ?: "",
                bonusCode = call.parameters["bonusCode"],
                amount = call.parameters["amount"] ?: "0"
            )

            call.respond(bet(sessionToken, payload))
        }

        get("/result.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            val payload = PragmaticBetDto(
                reference = call.parameters["reference"] ?: "",
                gameId = call.parameters["gameId"] ?: "",
                roundId = call.parameters["roundId"] ?: "",
                bonusCode = call.parameters["bonusCode"],
                amount = call.parameters["amount"] ?: "0",
                promoWinAmount = call.parameters["promoWinAmount"] ?: "0"
            )

            call.respond(result(sessionToken, payload))
        }

        get("/bonusWin.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            call.respond(balance(sessionToken))
        }

        get("/jackpotWin.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            call.respond(balance(sessionToken))
        }

        get("/refund.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            call.respond(refund(sessionToken))
        }

        get("/endRound.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            val roundId = call.parameters["roundId"] ?: ""

            call.respond(endRound(sessionToken, roundId))
        }

        get("/adjustment.html") {
            val sessionToken = call.parameters["token"]
                ?: return@get call.respond(PragmaticResponse.Error.SESSION_EXPIRED)

            val roundId = call.parameters["roundId"] ?: ""
            val reference = call.parameters["reference"] ?: ""
            val amount = call.parameters["amount"] ?: "0"
            val gameId = call.parameters["gameId"] ?: ""

            call.respond(adjustment(sessionToken, roundId, reference, amount, gameId))
        }
    }

    private suspend fun authenticate(sessionToken: String): PragmaticResponse {
        return runCatching {
            bus(FindSessionBalanceQuery(sessionToken))
        }.toBalanceResponse(userId = sessionToken)
    }

    private suspend fun balance(sessionToken: String): PragmaticResponse {
        return runCatching {
            bus(FindSessionBalanceQuery(sessionToken))
        }.toBalanceResponse()
    }

    private suspend fun bet(sessionToken: String, payload: PragmaticBetDto): PragmaticResponse {
        val amountMinor = providerToMinorUnits(payload.amount)

        return runCatching {
            bus(PlaceSpinSessionCommand(
                sessionToken = sessionToken,
                gameSymbol = payload.gameId,
                externalRoundId = payload.roundId,
                externalSpinId = payload.reference,
                freespinId = payload.bonusCode,
                amount = Amount(amountMinor)
            ))
        }.toBalanceResponse(transactionId = payload.reference)
    }

    private suspend fun result(sessionToken: String, payload: PragmaticBetDto): PragmaticResponse {
        val totalAmount = BigDecimal(payload.amount).add(BigDecimal(payload.promoWinAmount))
        val amountMinor = providerToMinorUnits(totalAmount)

        val result = runCatching {
            bus(SettleSpinSessionCommand(
                sessionToken = sessionToken,
                gameSymbol = payload.gameId,
                externalRoundId = payload.roundId,
                externalSpinId = payload.reference,
                freespinId = payload.bonusCode,
                amount = Amount(amountMinor)
            ))
        }

        return result.toBalanceResponse(transactionId = payload.reference)
    }

    private suspend fun endRound(sessionToken: String, roundId: String): PragmaticResponse {
        runCatching {
            bus(EndRoundSessionCommand(
                sessionToken = sessionToken,
                externalRoundId = roundId
            ))
        }

        return balance(sessionToken)
    }

    private suspend fun refund(sessionToken: String): PragmaticResponse {
        // TODO: implement RollbackSpinSessionCommand for proper refund handling
        return balance(sessionToken)
    }

    private suspend fun adjustment(
        sessionToken: String,
        roundId: String,
        reference: String,
        amount: String,
        gameId: String
    ): PragmaticResponse {
        val decimalAmount = BigDecimal(amount)
        val isDebit = decimalAmount < BigDecimal.ZERO

        return if (isDebit) {
            val amountMinor = providerToMinorUnits(decimalAmount.abs())
            runCatching {
                bus(PlaceSpinSessionCommand(
                    sessionToken = sessionToken,
                    gameSymbol = gameId,
                    externalRoundId = roundId,
                    externalSpinId = reference,
                    amount = Amount(amountMinor)
                ))
            }.toBalanceResponse()
        } else {
            val amountMinor = providerToMinorUnits(decimalAmount)
            runCatching {
                bus(SettleSpinSessionCommand(
                    sessionToken = sessionToken,
                    gameSymbol = gameId,
                    externalRoundId = roundId,
                    externalSpinId = reference,
                    amount = Amount(amountMinor)
                ))
            }.toBalanceResponse()
        }
    }

    private fun providerToMinorUnits(amount: String): Long {
        return BigDecimal(amount)
            .multiply(MINOR_UNIT_MULTIPLIER)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun providerToMinorUnits(amount: BigDecimal): Long {
        return amount
            .multiply(MINOR_UNIT_MULTIPLIER)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun minorUnitsToProvider(amount: Amount): String {
        return BigDecimal(amount.value)
            .divide(MINOR_UNIT_MULTIPLIER, 2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    private fun Result<PlayerBalance>.toBalanceResponse(
        transactionId: String? = null,
        userId: String? = null
    ): PragmaticResponse {
        return map { balance ->
            PragmaticResponse.Success(
                cash = minorUnitsToProvider(balance.realAmount),
                bonus = minorUnitsToProvider(balance.bonusAmount),
                currency = balance.currency.value,
                userId = userId,
                transactionId = transactionId
            )
        }.getOrElse { exception ->
            when (exception) {
                is SessionNotFoundException -> PragmaticResponse.Error.SESSION_EXPIRED
                is InsufficientBalanceException -> PragmaticResponse.Error.INSUFFICIENT_FUNDS
                is MaxPlaceSpinException -> PragmaticResponse.Error.BET_LIMIT_EXCEEDED
                else -> PragmaticResponse.Error.UNEXPECTED_ERROR
            }
        }
    }

    companion object {
        private val MINOR_UNIT_MULTIPLIER = BigDecimal(100)
    }
}
