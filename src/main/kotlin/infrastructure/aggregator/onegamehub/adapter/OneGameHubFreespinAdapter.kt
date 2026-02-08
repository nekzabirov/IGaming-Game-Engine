package infrastructure.aggregator.onegamehub.adapter

import application.port.outbound.AggregatorFreespinPort
import domain.aggregator.AggregatorInfo
import domain.common.error.AggregatorError
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import infrastructure.aggregator.shared.FreespinPresetValidator
import infrastructure.aggregator.onegamehub.client.OneGameHubHttpClient
import infrastructure.aggregator.onegamehub.client.dto.CancelFreespinDto
import infrastructure.aggregator.onegamehub.client.dto.CreateFreespinDto
import shared.value.Currency
import kotlinx.datetime.LocalDateTime

/**
 * OneGameHub implementation for freespin operations.
 */
class OneGameHubFreespinAdapter(
    private val aggregatorInfo: AggregatorInfo,
    private val providerCurrencyAdapter: OneGameHubCurrencyAdapter
) : AggregatorFreespinPort {

    private val config = OneGameHubConfig(aggregatorInfo.config)
    private val client = OneGameHubHttpClient(config)

    override suspend fun getPreset(gameSymbol: String): Result<Map<String, Any>> {
        return Result.success(
            mapOf(
                "quantity" to mapOf(
                    "minimal" to 1,
                ),
                "betAmount" to mapOf(
                    "minimal" to 10
                ),
                "lines" to mapOf(
                    "default" to 10,
                    "minimum" to 1,
                    "maximum" to 10
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

        val quantity = validatedValues["quantity"] ?: 0
        val betAmount = validatedValues["betAmount"] ?: 0
        val lines = validatedValues["lines"] ?: 0

        val response = client.createFreespin(
            CreateFreespinDto(
                id = referenceId,

                startAt = startAt,
                endAt = endAt,

                number = quantity,

                playerId = playerId,

                currency = currency.value,

                gameId = gameSymbol,

                bet = providerCurrencyAdapter.convertSystemToProvider(betAmount.toLong(), currency).toInt(),

                lineNumber = lines
            )
        ).getOrElse {
            return Result.failure(it)
        }

        if (!response.success) return Result.failure(
            AggregatorError("Cannot load game from aggregator OneGameHub. status : ${response.status}")
        )

        return Result.success(Unit)
    }

    override suspend fun cancelFreespin(
        referenceId: String,
    ): Result<Unit> {
        return client.cancelFreespin(CancelFreespinDto(referenceId))
            .map {
                Unit
            }
    }
}
