package events

import RabbitConfig
import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel as CoroutineChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

interface EventPublisher {

    /** Returns once the broker has confirmed the message on disk; throws if it did not. */
    suspend fun publish(event: AppEvent)
}

/** For entrypoints that must never emit events (the catalog sync). */
object NoOpEventPublisher : EventPublisher {
    override suspend fun publish(event: AppEvent) = Unit
}

/** The single long-lived connection; the publisher and the consumer each take their own channel. */
fun rabbitConnection(config: RabbitConfig): Connection =
    ConnectionFactory().apply { setUri(config.uri) }.newConnection()

/** Declares the shared topic exchange once at startup. */
fun declareEventExchange(channel: Channel, exchange: String) {
    channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true)
}

/**
 * Publishes on a dedicated confirm-mode channel: PERSISTENT deliveries, and a publish returns only
 * once the broker confirmed it. A Java-client channel is single-threaded, so publishes are funnelled
 * through one worker coroutine — which also lets it confirm a whole BATCH with one round trip when
 * events queue up under load, instead of paying a broker round trip per event under a lock.
 *
 * A channel closed by a channel-level AMQP error is re-created and the batch retried once.
 */
class RabbitEventPublisher(
    private val connection: Connection,
    private val exchange: String,
) : EventPublisher {

    private class Pending(val route: String, val body: ByteArray, val done: CompletableDeferred<Unit>)

    private val queue = CoroutineChannel<Pending>(CoroutineChannel.UNLIMITED)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("event-publisher"))

    private var channel: Channel? = null

    init {
        scope.launch { loop() }
    }

    override suspend fun publish(event: AppEvent) {
        val envelope = buildJsonObject {
            put("playerId", JsonPrimitive(event.playerId))
            put("data", event.data())
        }
        val body = appJson.encodeToString(JsonObject.serializer(), envelope).toByteArray()
        val pending = Pending(event.route, body, CompletableDeferred())
        queue.send(pending)
        pending.done.await()
    }

    private suspend fun loop() {
        while (true) {
            val batch = mutableListOf(queue.receive())
            while (batch.size < MAX_BATCH) {
                batch += queue.tryReceive().getOrNull() ?: break
            }
            try {
                publishConfirmed(batch)
                batch.forEach { it.done.complete(Unit) }
            } catch (e: Exception) {
                batch.forEach { it.done.completeExceptionally(e) }
            }
        }
    }

    private fun publishConfirmed(batch: List<Pending>) {
        try {
            send(batch)
        } catch (e: AlreadyClosedException) {
            // The channel died mid-publish; the connection is still up, so re-create the channel
            // and retry once. A second failure propagates to every caller of the batch.
            channel = null
            send(batch)
        }
    }

    private fun send(batch: List<Pending>) {
        val current = channel?.takeIf { it.isOpen }
            ?: connection.createChannel().apply { confirmSelect() }.also { channel = it }
        batch.forEach { current.basicPublish(exchange, it.route, MessageProperties.PERSISTENT_BASIC, it.body) }
        try {
            current.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS)
        } catch (e: Exception) {
            // waitForConfirmsOrDie closes the channel on a nack/timeout; do not reuse it.
            channel = null
            throw e
        }
    }

    private companion object {
        const val CONFIRM_TIMEOUT_MS = 5_000L

        const val MAX_BATCH = 100
    }
}
