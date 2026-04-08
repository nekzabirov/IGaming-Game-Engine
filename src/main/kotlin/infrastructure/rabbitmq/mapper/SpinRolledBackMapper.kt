package infrastructure.rabbitmq.mapper

import domain.event.SpinRolledBack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpinRollbackPayload(
    val gameIdentity: String,

    val playerId: String,

    val refundAmount: Long,

    val currency: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object SpinRolledBackMapper {
    const val ROUTING_KEY = "spin.rollback"

    fun toPayload(event: SpinRolledBack): String {
        val spin = event.spin
        val session = spin.round.session
        val payload = SpinRollbackPayload(
            gameIdentity = session.gameVariant.symbol.value,
            playerId = session.playerId.value,
            refundAmount = spin.amount.value,
            currency = session.currency.value,
            freeSpinId = spin.round.freespinId?.value,
        )
        return Json.encodeToString(SpinRollbackPayload.serializer(), payload)
    }
}
