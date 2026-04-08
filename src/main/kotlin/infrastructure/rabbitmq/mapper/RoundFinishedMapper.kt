package infrastructure.rabbitmq.mapper

import domain.event.RoundFinished
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RoundFinishedPayload(
    val gameIdentity: String,

    val playerId: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object RoundFinishedMapper {
    const val ROUTING_KEY = "round.finished"

    fun toPayload(event: RoundFinished): String {
        val session = event.round.session
        val payload = RoundFinishedPayload(
            gameIdentity = session.gameVariant.symbol.value,
            playerId = session.playerId.value,
            freeSpinId = event.round.freespinId?.value,
        )
        return Json.encodeToString(RoundFinishedPayload.serializer(), payload)
    }
}
