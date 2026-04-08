package infrastructure.rabbitmq

import application.port.external.IEventPort
import domain.event.DomainEvent
import domain.event.RoundFinished
import domain.event.SessionOpened
import domain.event.SpinPlaced
import domain.event.SpinRolledBack
import domain.event.SpinSettled
import infrastructure.rabbitmq.mapper.RoundFinishedMapper
import infrastructure.rabbitmq.mapper.SessionOpenedMapper
import infrastructure.rabbitmq.mapper.SpinPlacedMapper
import infrastructure.rabbitmq.mapper.SpinRolledBackMapper
import infrastructure.rabbitmq.mapper.SpinSettledMapper
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

/**
 * Publishes [DomainEvent]s to RabbitMQ. The exhaustive `when` makes the compiler enforce
 * that every new domain event subtype gets a routing key + payload mapping — there is no
 * intermediate `ApplicationEvent` translation layer.
 */
class RabbitMqEventPublisher(
    private val application: Application,
    private val config: RabbitMqConfig,
) : IEventPort {

    private val logger = LoggerFactory.getLogger(RabbitMqEventPublisher::class.java)

    override suspend fun publish(event: DomainEvent) {
        val (routingKey, payload) = when (event) {
            is SessionOpened  -> SessionOpenedMapper.ROUTING_KEY  to SessionOpenedMapper.toPayload(event)
            is SpinPlaced     -> SpinPlacedMapper.ROUTING_KEY     to SpinPlacedMapper.toPayload(event)
            is SpinSettled    -> SpinSettledMapper.ROUTING_KEY    to SpinSettledMapper.toPayload(event)
            is SpinRolledBack -> SpinRolledBackMapper.ROUTING_KEY to SpinRolledBackMapper.toPayload(event)
            is RoundFinished  -> RoundFinishedMapper.ROUTING_KEY  to RoundFinishedMapper.toPayload(event)
        }

        logger.info("Publishing event [$routingKey]: $payload")

        application.rabbitmq {
            basicPublish {
                exchange = config.exchange
                this.routingKey = routingKey
                message { payload }
            }
        }
    }
}
