package infrastructure.rabbitmq

import application.port.external.IPlayerLimitPort
import domain.vo.Amount
import domain.vo.PlayerId
import infrastructure.rabbitmq.mapper.SpinPlacedPayload
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueBind
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

class PlaceSpinEventConsumer(
    private val application: Application,
    private val config: RabbitMqConfig,
    private val playerLimitPort: IPlayerLimitPort,
) {

    private val logger = LoggerFactory.getLogger(PlaceSpinEventConsumer::class.java)

    companion object {
        private const val QUEUE_NAME = "casino-engine.player-limit.spin-placed"
        private const val ROUTING_KEY = "spin.placed"
    }

    fun start() {
        application.rabbitmq {
            queueBind {
                queue = QUEUE_NAME
                exchange = config.exchange
                routingKey = ROUTING_KEY
                queueDeclare {
                    queue = QUEUE_NAME
                    durable = true
                }
            }

            basicConsume {
                queue = QUEUE_NAME
                autoAck = true
                deliverCallback<SpinPlacedPayload> { message ->
                    handlePlacedSpin(message.body)
                }
            }
        }
    }

    private suspend fun handlePlacedSpin(payload: SpinPlacedPayload) {
        val playerId = PlayerId(payload.playerId)
        val spinAmount = Amount(payload.amount)

        val currentLimit = playerLimitPort.getMaxPlaceAmount(playerId) ?: return

        val newLimit = Amount(maxOf(0, currentLimit.value - spinAmount.value))

        playerLimitPort.saveMaxPlaceAmount(playerId, newLimit)

        logger.info("Decreased player limit for [${playerId.value}]: $currentLimit -> $newLimit")
    }
}
