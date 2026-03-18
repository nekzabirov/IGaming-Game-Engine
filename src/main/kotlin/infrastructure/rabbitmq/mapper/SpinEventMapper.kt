package infrastructure.rabbitmq.mapper

import application.event.SpinEvent
import domain.model.SpinType
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

@Serializable
data class SpinSettledPayload(
    val gameIdentity: String,

    val amount: Long,

    val currency: String,

    val playerId: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class SpinRollbackPayload(
    val gameIdentity: String,

    val playerId: String,

    val refundAmount: Long,

    val currency: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object SpinEventMapper {
    private const val ROUTING_KEY_PLACED = "spin.placed"
    private const val ROUTING_KEY_SETTLED = "spin.settled"
    private const val ROUTING_KEY_ROLLBACK = "spin.rollback"

    fun routingKey(event: SpinEvent): String = when (event.spin.type) {
        SpinType.PLACE -> ROUTING_KEY_PLACED
        SpinType.SETTLE -> ROUTING_KEY_SETTLED
        SpinType.ROLLBACK -> ROUTING_KEY_ROLLBACK
    }

    fun toPayload(event: SpinEvent): String {
        val spin = event.spin
        val session = spin.round.session

        return when (spin.type) {
            SpinType.PLACE -> Json.encodeToString(
                SpinPlacedPayload.serializer(),
                SpinPlacedPayload(
                    gameIdentity = session.gameVariant.symbol,
                    amount = spin.amount.value,
                    currency = session.currency.value,
                    playerId = session.playerId.value,
                    freeSpinId = spin.round.freespinId
                )
            )

            SpinType.SETTLE -> Json.encodeToString(
                SpinSettledPayload.serializer(),
                SpinSettledPayload(
                    gameIdentity = session.gameVariant.symbol,
                    amount = spin.amount.value,
                    currency = session.currency.value,
                    playerId = session.playerId.value,
                    freeSpinId = spin.round.freespinId
                )
            )

            SpinType.ROLLBACK -> Json.encodeToString(
                SpinRollbackPayload.serializer(),
                SpinRollbackPayload(
                    gameIdentity = session.gameVariant.symbol,
                    playerId = session.playerId.value,
                    refundAmount = spin.amount.value,
                    currency = session.currency.value,
                    freeSpinId = spin.round.freespinId
                )
            )
        }
    }
}
