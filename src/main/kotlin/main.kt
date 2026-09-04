import api.grpc.config.configureKoin
import api.grpc.configureGrpc
import api.grpc.service.AggregatorGrpcService
import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.CasinoGameGrpcService
import api.grpc.service.CasinoProviderGrpcService
import api.grpc.service.WinnerGrpcService
import api.webhook.configureRestInspector
import api.webhook.configureWebhook
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
import com.rabbitmq.client.Channel
import infrastructure.aggregator.onegamehub.webhook.OneGameHubWebhook
import infrastructure.aggregator.pragmatic.webhook.PragmaticWebhook
import infrastructure.rabbitmq.EVENT_EXCHANGE
import infrastructure.rabbitmq.PlaceSpinEventConsumer
import infrastructure.rabbitmq.declareEventExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.nekgamebling.Main")

fun main() {
    // grpc-netty-shaded prefers its bundled BoringSSL for TLS, and loading it in this image kills
    // the JVM outright: SIGSEGV inside netty_internal_tcnative_SSLContext_JNI_OnLoad. The flag has
    // to be set before any netty SSL class loads — GrpcSslContexts probes OpenSsl during its own
    // static init, so choosing the JDK provider at the call site is already too late.
    System.setProperty("io.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl", "true")

    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(CIO, port = httpPort(), host = "0.0.0.0") {
        configureKoin()
        configureDatabase()
        configureSerialization()
        configureCallLogging()
        configureRestInspector()
        configureRabbitMqTopology()
        configureWebhook()
        configureGrpc()
        configureConsumers()
    }.start(wait = true)
}

private fun Application.configureDatabase() {
    val config = get<DatabaseConfig>()
    DatabaseFactory.init(config)
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

private fun Application.configureRabbitMqTopology() {
    val channel = get<Channel>()
    declareEventExchange(channel)
    logger.info("RabbitMQ topology ready: exchange={}", EVENT_EXCHANGE)
}

private fun Application.configureConsumers() {
    // Instantiating an AppEventConsumer auto-declares its queue, binds it, and starts
    // consuming in the base class init block — resolving it from Koin is enough.
    get<PlaceSpinEventConsumer>()
}

private fun httpPort(): Int = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080