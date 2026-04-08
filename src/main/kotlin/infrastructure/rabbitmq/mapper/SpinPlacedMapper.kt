package infrastructure.rabbitmq.mapper

import domain.event.SpinPlaced
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpinPlacedPayload(
    val gameIdentity: String,

    val amount: Long,

    val currency: String,

    val playerId: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object SpinPlacedMapper {
    const val ROUTING_KEY = "spin.placed"

    fun toPayload(event: SpinPlaced): String {
        val spin = event.spin
        val session = spin.round.session
        val payload = SpinPlacedPayload(
            gameIdentity = session.gameVariant.symbol.value,
            amount = spin.amount.value,
            currency = session.currency.value,
            playerId = session.playerId.value,
            freeSpinId = spin.round.freespinId?.value,
        )
        return Json.encodeToString(SpinPlacedPayload.serializer(), payload)
    }
}
