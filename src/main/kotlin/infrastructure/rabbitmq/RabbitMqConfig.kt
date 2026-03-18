package infrastructure.rabbitmq

data class RabbitMqConfig(
    val uri: String,
    val exchange: String
)
