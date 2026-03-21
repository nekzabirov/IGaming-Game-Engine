package infrastructure.aggregator.onegamehub.webhook

import application.cqrs.Bus
import application.cqrs.session.FindSessionBalanceQuery
import application.cqrs.session.EndRoundSessionCommand
import application.cqrs.session.PlaceSpinSessionCommand
import application.cqrs.session.SettleSpinSessionCommand
import domain.exception.forbidden.InsufficientBalanceException
import domain.exception.forbidden.MaxPlaceSpinException
import domain.exception.notfound.SessionNotFoundException
import domain.model.PlayerBalance
import domain.vo.Amount
import infrastructure.aggregator.onegamehub.webhook.dto.OneGameHubResponse
import io.ktor.http.Parameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class  OneGameHubWebhook(private val bus: Bus) {

    private val Parameters.amount get() = this["amount"]!!.toLong()
    private val Parameters.gameSymbol get() = this["game_id"]!!
    private val Parameters.transactionId get() = this["transaction_id"]!!
    private val Parameters.roundId get() = this["round_id"]!!
    private val Parameters.freespinId get() = this["freerounds_id"]
    private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

    fun Route.route() = post("/onegamehub") {
        val action = call.queryParameters["action"]
        val sessionToken = call.queryParameters["extra"]

        if (action == null || sessionToken == null) {
            call.respond(OneGameHubResponse.Error.UNEXPECTED_ERROR)
            return@post
        }

        val response = when (action) {
            "balance" -> balance(sessionToken)
            "bet" -> bet(sessionToken, call.parameters)
            "win" -> win(sessionToken, call.parameters)
            else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }

        call.respond(response)
    }

    private suspend fun balance(sessionToken: String): OneGameHubResponse {
        return runCatching {
            bus(FindSessionBalanceQuery(sessionToken))
        }.toResponse()
    }

    private suspend fun bet(sessionToken: String, parameters: Parameters): OneGameHubResponse {
        return runCatching {
            bus(PlaceSpinSessionCommand(
                sessionToken = sessionToken,
                gameSymbol = parameters.gameSymbol,
                externalRoundId = parameters.roundId,
                externalSpinId = parameters.transactionId,
                freespinId = parameters.freespinId,
                amount = Amount(parameters.amount)
            ))
        }.toResponse()
    }

    private suspend fun win(sessionToken: String, parameters: Parameters): OneGameHubResponse {
        return runCatching {
            bus(SettleSpinSessionCommand(
                sessionToken = sessionToken,
                gameSymbol = parameters.gameSymbol,
                externalRoundId = parameters.roundId,
                externalSpinId = parameters.transactionId,
                freespinId = parameters.freespinId,
                amount = Amount(parameters.amount)
            ))
        }.onSuccess { _ ->
            if (parameters.isRoundEnd) {
                runCatching {
                    bus(EndRoundSessionCommand(
                        sessionToken = sessionToken,
                        externalRoundId = parameters.roundId
                    ))
                }
            }
        }.toResponse()
    }

    private fun Result<PlayerBalance>.toResponse(): OneGameHubResponse {
        return map { balance ->
            OneGameHubResponse.Success(
                balance = balance.total.value.toInt(),
                currency = balance.currency.value
            )
        }.getOrElse { exception ->
            when (exception) {
                is SessionNotFoundException -> OneGameHubResponse.Error.SESSION_TIMEOUT
                is InsufficientBalanceException -> OneGameHubResponse.Error.INSUFFICIENT_FUNDS
                is MaxPlaceSpinException -> OneGameHubResponse.Error.EXCEED_WAGER_LIMIT
                else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
            }
        }
    }
}
