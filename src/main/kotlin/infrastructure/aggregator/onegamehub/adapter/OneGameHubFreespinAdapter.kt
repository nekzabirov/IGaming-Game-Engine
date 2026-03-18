package infrastructure.aggregator.onegamehub.adapter

import application.port.external.IFreespinPort
import domain.vo.Currency
import domain.vo.PlayerId
import infrastructure.aggregator.onegamehub.OneGameHubConfig
import infrastructure.aggregator.onegamehub.client.OneGameHubHttpClient
import infrastructure.aggregator.onegamehub.client.dto.CancelFreespinDto
import infrastructure.aggregator.onegamehub.client.dto.CreateFreespinDto
import kotlinx.datetime.LocalDateTime

class OneGameHubFreespinAdapter(
    config: OneGameHubConfig,
) : IFreespinPort {

    private val client = OneGameHubHttpClient(config)

    override suspend fun getPreset(gameSymbol: String): Map<String, Any> {
        val response = client.listGames()

        check(response.success) { "OneGameHub listGames failed with status ${response.status}" }

        val game = response.response
            ?.firstOrNull { it.id == gameSymbol }
            ?: error("Game $gameSymbol not found in OneGameHub")

        return mapOf(
            "paylines" to game.paylines,
            "freespinEnable" to game.freespinEnable
        )
    }

    override suspend fun create(
        presetValue: Map<String, Any>,
        referenceId: String,
        playerId: PlayerId,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ) {
        val bet = (presetValue["bet"] as? Number)?.toInt() ?: 0
        val number = (presetValue["number"] as? Number)?.toInt() ?: 0
        val lineNumber = (presetValue["paylines"] as? Number)?.toInt() ?: 0

        val payload = CreateFreespinDto(
            id = referenceId,
            startAt = startAt,
            endAt = endAt,
            number = number,
            playerId = playerId.value,
            currency = currency.value,
            gameId = gameSymbol,
            bet = bet,
            lineNumber = lineNumber
        )

        val response = client.createFreespin(payload)

        check(response.success) { "OneGameHub createFreespin failed with status ${response.status}" }
    }

    override suspend fun cancel(referenceId: String) {
        val payload = CancelFreespinDto(id = referenceId)

        val response = client.cancelFreespin(payload)

        check(response.success) { "OneGameHub cancelFreespin failed with status ${response.status}" }
    }
}
