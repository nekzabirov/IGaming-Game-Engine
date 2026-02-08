package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorFreespinPort
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import infrastructure.aggregator.pragmatic.client.dto.CreateFreespinDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinBetValueDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinGameDto
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.shared.FreespinPresetValidator
import infrastructure.aggregator.shared.ProviderCurrencyAdapter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import shared.value.Currency

/**
 * Pragmatic implementation for freespin operations.
 */
class PragmaticFreespinAdapter(
    aggregatorInfo: AggregatorInfo,
    private val providerCurrencyAdapter: ProviderCurrencyAdapter
) : AggregatorFreespinPort {

    private val client = PragmaticHttpClient(PragmaticConfig(aggregatorInfo.config))

    override suspend fun getPreset(gameSymbol: String): Result<Map<String, Any>> {
        return Result.success(
            mapOf(
                "totalBet" to mapOf(
                    "minimal" to 100,
                ),
                "rounds" to mapOf(
                    "minimal" to 10
                )
            )
        )
    }

    override suspend fun createFreespin(
        presetValue: Map<String, Int>,
        referenceId: String,
        playerId: String,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Result<Unit> {
        val mainPreset = getPreset(gameSymbol).getOrElse {
            return Result.failure(it)
        }

        val validatedValues = FreespinPresetValidator.validate(presetValue, mainPreset).getOrElse {
            return Result.failure(it)
        }

        val rounds = validatedValues["rounds"] ?: 0
        val totalBet = validatedValues["totalBet"] ?: 0

        // Convert totalBet from system format to provider format (real currency units)
        val totalBetDecimal = providerCurrencyAdapter.convertSystemToProvider(
            totalBet.toLong(),
            currency
        ).toDouble()

        // Convert LocalDateTime to Unix timestamp (seconds)
        val startTimestamp = startAt.toInstant(TimeZone.UTC).epochSeconds
        val expirationTimestamp = endAt.toInstant(TimeZone.UTC).epochSeconds

        val payload = CreateFreespinDto(
            bonusCode = referenceId,
            playerId = playerId,
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

        return client.createFreespin(payload)
    }

    override suspend fun cancelFreespin(
        referenceId: String,
    ): Result<Unit> {
        return client.cancelFreespin(referenceId)
    }
}
