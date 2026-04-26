package infrastructure.rabbitmq

data class RabbitMqConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val tls: Boolean,
) {
    val uri: String get() {
        val scheme = if (tls) "amqps" else "amqp"
        return "$scheme://$user:$password@$host:$port"
    }
}
