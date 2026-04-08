package infrastructure.rabbitmq.mapper

import domain.event.SpinSettled
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpinSettledPayload(
    val gameIdentity: String,

    val amount: Long,

    val currency: String,

    val playerId: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object SpinSettledMapper {
    const val ROUTING_KEY = "spin.settled"

    fun toPayload(event: SpinSettled): String {
        val spin = event.spin
        val session = spin.round.session
        val payload = SpinSettledPayload(
            gameIdentity = session.gameVariant.symbol.value,
            amount = spin.amount.value,
            currency = session.currency.value,
            playerId = session.playerId.value,
            freeSpinId = spin.round.freespinId?.value,
        )
        return Json.encodeToString(SpinSettledPayload.serializer(), payload)
    }
}
