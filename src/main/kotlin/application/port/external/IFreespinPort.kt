package application.port.external

import domain.vo.Currency
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime

interface IFreespinPort {

    suspend fun getPreset(gameSymbol: String): Map<String, Any>

    suspend fun create(
        presetValue: Map<String, Any>,
        referenceId: String,
        playerId: PlayerId,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    )

    suspend fun cancel(referenceId: String)
}