package infrastructure.rabbitmq.mapper

import application.event.RoundEndEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RoundEndPayload(
    val gameIdentity: String,

    val playerId: String,

    val freeSpinId: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
)

object RoundEndEventMapper {
    const val ROUTING_KEY = "spin.end"

    fun toPayload(event: RoundEndEvent): String {
        val session = event.round.session
        val payload = RoundEndPayload(
            gameIdentity = session.gameVariant.symbol,
            playerId = session.playerId.value,
            freeSpinId = event.round.freespinId
        )
        return Json.encodeToString(RoundEndPayload.serializer(), payload)
    }
}
