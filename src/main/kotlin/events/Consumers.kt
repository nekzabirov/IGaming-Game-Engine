package events

import clients.PlayerLimits
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import db.SpinType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory

/**
 * Wears the player's bet cap down by every PLACE, from our own spin events. Auto-ack, at-most-once:
 * a failed delivery is logged and dropped, never requeued.
 *
 * The queue name is the historical class name — a durable queue by that name exists on every
 * broker, bound to `spin.events`; renaming it would leave the old queue filling up unread.
 */
class PlaceSpinEventConsumer(
    channel: Channel,
    exchange: String,
    private val limits: PlayerLimits,
) {

    init {
        channel.queueDeclare(QUEUE, true, false, false, null)
        channel.queueBind(QUEUE, exchange, SpinEvent.ROUTE)
        val callback = DeliverCallback { _, delivery ->
            // A poison message or a failing handler must NEVER escape this callback: the client
            // closes the channel on an uncaught consumer exception (the 2026-06-09 outage).
            try {
                val envelope = appJson.parseToJsonElement(delivery.body.decodeToString()).jsonObject
                val spin = appJson.decodeFromJsonElement(SpinWireSerializer, envelope.getValue("data"))
                runBlocking { handle(spin) }
            } catch (e: Exception) {
                log.error("Dropping poison/failed delivery on queue '{}': {}", QUEUE, e.message, e)
            }
        }
        channel.basicConsume(QUEUE, true, callback) { _ -> }
    }

    private suspend fun handle(spin: SpinPayload) {
        if (spin.type != SpinType.PLACE) return
        limits.decrease(spin.round.playerId, spin.amount)
    }

    private companion object {
        const val QUEUE = "PlaceSpinEventConsumer"

        val log = LoggerFactory.getLogger(PlaceSpinEventConsumer::class.java)
    }
}
