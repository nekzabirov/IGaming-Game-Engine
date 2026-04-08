import api.grpc.config.configureKoin
import api.grpc.configureGrpc
import api.grpc.service.AggregatorGrpcService
import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.GameGrpcService
import api.grpc.service.ProviderGrpcService
import api.grpc.service.WinnerGrpcService
import api.rest.configureRouting
import api.rest.httpPort
import infrastructure.persistence.DatabaseConfig
import infrastructure.persistence.DatabaseFactory
import io.grpc.ServerBuilder
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import infrastructure.aggregator.onegamehub.webhook.OneGameHubWebhook
import infrastructure.aggregator.pragmatic.webhook.PragmaticWebhook
import infrastructure.rabbitmq.PlaceSpinEventConsumer
import infrastructure.rabbitmq.RabbitMqConfig
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.nekgamebling.Main")

fun main() {
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(CIO, port = httpPort(), host = "0.0.0.0") {
        configureKoin()
        configureDatabase()
        configureSerialization()
        configureCallLogging()
        configureRabbitMq()
        configureRouting()
        configureGrpc()
        configureConsumers()
    }.start(wait = true)
}

private fun Application.configureDatabase() {
    val config = get<DatabaseConfig>()
    DatabaseFactory.init(config)
    runBlocking {
        DatabaseFactory.createTables()
    }
}

private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

private fun Application.configureCallLogging() {
    install(CallLogging)
}

private fun Application.configureRabbitMq() {
    val config = get<RabbitMqConfig>()
    install(RabbitMQ) {
        uri = config.uri
    }
}

private fun Application.configureConsumers() {
    val consumer = get<PlaceSpinEventConsumer>()
    consumer.start()
}