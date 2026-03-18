package infrastructure.aggregator.pragmatic.adapter

import application.port.external.IFreespinPort
import domain.vo.Currency
import domain.vo.PlayerId
import infrastructure.aggregator.pragmatic.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import infrastructure.aggregator.pragmatic.client.dto.CreateFreespinDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinBetValueDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinGameDto
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class PragmaticFreespinAdapter(
    config: PragmaticConfig,
) : IFreespinPort {

    private val client = PragmaticHttpClient(config)

    override suspend fun getPreset(gameSymbol: String): Map<String, Any> {
        return mapOf(
            "totalBet" to mapOf(
                "minimal" to 100
            ),
            "rounds" to mapOf(
                "minimal" to 10
            )
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
        val rounds = (presetValue["rounds"] as? Number)?.toInt() ?: 10
        val totalBet = (presetValue["totalBet"] as? Number)?.toInt() ?: 100

        val totalBetDecimal = totalBet.toDouble() / MINOR_UNIT_DIVISOR

        val startTimestamp = startAt.toInstant(TimeZone.UTC).epochSeconds
        val expirationTimestamp = endAt.toInstant(TimeZone.UTC).epochSeconds

        val payload = CreateFreespinDto(
            bonusCode = referenceId,
            playerId = playerId.value,
            currency = currency.value,
            rounds = rounds,
            startTimestamp = startTimestamp,
            expirationTimestamp = expirationTimestamp,
            gameList = listOf(
                FreespinGameDto(
                    gameId = gameSymbol,
                    betValues = listOf(
                        FreespinBetValueDto(
                            currency = currency.value,
                            totalBet = totalBetDecimal
                        )
                    )
                )
            )
        )

        client.createFreespin(payload)
    }

    override suspend fun cancel(referenceId: String) {
        client.cancelFreespin(referenceId)
    }

    companion object {
        private const val MINOR_UNIT_DIVISOR = 100.0
    }
}
