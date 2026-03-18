package infrastructure.rabbitmq.mapper

import application.event.SessionOpenEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SessionOpenedPayload(
    val sessionId: Long,

    val gameId: String,

    val gameIdentity: String,

    val playerId: String,

    val currency: String,

    val platform: String,

    val timestamp: Long = System.currentTimeMillis(),
)

object SessionOpenEventMapper {
    const val ROUTING_KEY = "session.opened"

    fun toPayload(event: SessionOpenEvent): String {
        val payload = SessionOpenedPayload(
            sessionId = event.session.id,
            gameId = event.session.gameVariant.game.identity.value,
            gameIdentity = event.session.gameVariant.symbol,
            playerId = event.session.playerId.value,
            currency = event.session.currency.value,
            platform = event.session.platform.name
        )
        return Json.encodeToString(SessionOpenedPayload.serializer(), payload)
    }
}
