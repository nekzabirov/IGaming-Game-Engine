package infrastructure.aggregator.pateplay.adapter

import application.port.external.IFreespinPort
import domain.vo.Currency
import domain.vo.PlayerId
import infrastructure.aggregator.pateplay.PateplayConfig
import infrastructure.aggregator.pateplay.client.PateplayHttpClient
import infrastructure.aggregator.pateplay.client.dto.CreateFreespinRequestDto
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import java.math.RoundingMode

class PateplayFreespinAdapter(
    config: PateplayConfig,
) : IFreespinPort {

    private val client = PateplayHttpClient(config)

    override suspend fun getPreset(gameSymbol: String): Map<String, Any> {
        return mapOf(
            "stake" to mapOf(
                "minimal" to 100
            ),
            "rounds" to mapOf(
                "minimal" to 1
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
        val rounds = (presetValue["rounds"] as? Number)?.toInt() ?: 1
        val stake = (presetValue["stake"] as? Number)?.toLong() ?: 100L

        val stakeDecimal = BigDecimal(stake)
            .divide(MINOR_UNIT_DIVISOR, 2, RoundingMode.HALF_UP)
            .toPlainString()

        val startTimestamp = startAt.toInstant(TimeZone.UTC).epochSeconds
        val endTimestamp = endAt.toInstant(TimeZone.UTC).epochSeconds
        val ttlSeconds = endTimestamp - startTimestamp

        val expiresAt = "${endAt}Z"

        val payload = CreateFreespinRequestDto(
            referenceId = referenceId,
            playerId = playerId.value,
            currency = currency.value,
            ttlSeconds = ttlSeconds,
            gameSymbol = gameSymbol,
            stake = stakeDecimal,
            rounds = rounds,
            expiresAt = expiresAt
        )

        client.createFreespin(payload)
    }

    override suspend fun cancel(referenceId: String) {
        val bonusId = requireNotNull(referenceId.toLongOrNull()) {
            "Invalid bonus reference ID: $referenceId"
        }

        client.cancelFreespin(bonusId)
    }

    companion object {
        private val MINOR_UNIT_DIVISOR = BigDecimal(100)
    }
}
