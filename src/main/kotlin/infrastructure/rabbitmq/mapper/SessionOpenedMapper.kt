package infrastructure.rabbitmq.mapper

import domain.event.SessionOpened
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

object SessionOpenedMapper {
    const val ROUTING_KEY = "session.opened"

    fun toPayload(event: SessionOpened): String {
        val session = event.session
        val payload = SessionOpenedPayload(
            sessionId = session.id,
            gameId = session.gameVariant.game.identity.value,
            gameIdentity = session.gameVariant.symbol.value,
            playerId = session.playerId.value,
            currency = session.currency.value,
            platform = session.platform.name,
        )
        return Json.encodeToString(SessionOpenedPayload.serializer(), payload)
    }
}
