package infrastructure.rabbitmq

import application.event.ApplicationEvent
import application.event.RoundEndEvent
import application.event.SessionOpenEvent
import application.event.SpinEvent
import application.port.external.IEventPort
import infrastructure.rabbitmq.mapper.RoundEndEventMapper
import infrastructure.rabbitmq.mapper.SessionOpenEventMapper
import infrastructure.rabbitmq.mapper.SpinEventMapper
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

class RabbitMqEventPublisher(
    private val application: Application,
    private val config: RabbitMqConfig
) : IEventPort {

    private val logger = LoggerFactory.getLogger(RabbitMqEventPublisher::class.java)

    override suspend fun publish(event: ApplicationEvent) {
        val (routingKey, payload) = when (event) {
            is SessionOpenEvent -> SessionOpenEventMapper.ROUTING_KEY to SessionOpenEventMapper.toPayload(event)
            is SpinEvent -> SpinEventMapper.routingKey(event) to SpinEventMapper.toPayload(event)
            is RoundEndEvent -> RoundEndEventMapper.ROUTING_KEY to RoundEndEventMapper.toPayload(event)
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
