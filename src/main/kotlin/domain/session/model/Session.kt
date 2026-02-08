package domain.session.model

import kotlinx.datetime.LocalDateTime
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform
import java.util.UUID

/**
 * Player gaming session entity.
 */
data class Session(
    val id: UUID = UUID.randomUUID(),
    val gameId: UUID,
    val aggregatorId: UUID,
    val playerId: String,
    val token: String,
    val externalToken: String?,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform
) {
    init {
        require(playerId.isNotBlank()) { "Player ID cannot be blank" }
        require(token.isNotBlank()) { "Session token cannot be blank" }
    }
}

/**
 * Betting round within a session.
 */
data class Round(
    val id: UUID = UUID.randomUUID(),
    val sessionId: UUID,
    val gameId: UUID,
    val extId: String,
    val finished: Boolean = false,
    val createdAt: LocalDateTime,
    val finishedAt: LocalDateTime? = null
) {
    fun finish(finishedAt: LocalDateTime): Round = copy(finished = true, finishedAt = finishedAt)
}

/**
 * Individual spin/bet within a round.
 */
data class Spin(
    val id: UUID = UUID.randomUUID(),
    val roundId: UUID,
    val type: domain.common.value.SpinType,
    val amount: Long,
    val realAmount: Long,
    val bonusAmount: Long,
    val extId: String,
    val referenceId: UUID? = null,
    val freeSpinId: String? = null
)

/**
 * Player balance.
 */
data class Balance(
    val real: Long,
    val bonus: Long,
    val currency: Currency
) {
    val totalAmount: Long get() = real + bonus
}
