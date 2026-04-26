package infrastructure.rabbitmq

data class RabbitMqConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
) {
    val uri: String get() = "amqp://$user:$password@$host:$port"
}
