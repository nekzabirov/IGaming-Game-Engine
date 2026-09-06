package plugins

import clients.PlayerLimits
import com.rabbitmq.client.Connection
import events.PlaceSpinEventConsumer
import events.declareEventExchange
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("messaging")

/**
 * Declares the shared topic exchange and starts the one consumer on its own channel — the
 * publisher owns a separate confirm-mode channel, so a consumer-side error can never take
 * publishing down with it.
 */
fun Application.configureMessaging(connection: Connection, exchange: String, limits: PlayerLimits) {
    val channel = connection.createChannel()
    declareEventExchange(channel, exchange)
    PlaceSpinEventConsumer(channel, exchange, limits)

    log.info("RabbitMQ topology ready: exchange={}", exchange)

    monitor.subscribe(ApplicationStopping) {
        runCatching { connection.close() }
    }
}
