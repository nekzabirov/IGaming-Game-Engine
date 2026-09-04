import application.Bus
import application.command.aggregator.SyncAllActiveAggregatorCommand
import application.port.external.IEventPublisherPort
import infrastructure.koin.aggregatorModule
import infrastructure.koin.busModule
import infrastructure.koin.configModule
import infrastructure.koin.externalModule
import infrastructure.koin.handlerModule
import infrastructure.koin.persistenceModule
import infrastructure.koin.usecaseModule
import infrastructure.persistence.DatabaseConfig
import infrastructure.persistence.DatabaseFactory
import infrastructure.rabbitmq.NoOpAppEventPublisher
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.nekgamebling.SyncJob")

private val syncOverrideModule = module {
    // SyncJob does not publish events and must not open a RabbitMQ channel.
    single<IEventPublisherPort> { NoOpAppEventPublisher }
}

fun main() {
    // grpc-netty-shaded prefers its bundled BoringSSL for TLS, and loading it in this image kills
    // the JVM outright: SIGSEGV inside netty_internal_tcnative_SSLContext_JNI_OnLoad. The flag has
    // to be set before any netty SSL class loads — GrpcSslContexts probes OpenSsl during its own
    // static init, so choosing the JDK provider at the call site is already too late.
    System.setProperty("io.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl", "true")

    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    val koinApp = startKoin {
        slf4jLogger()
        allowOverride(true)
        modules(
            configModule,
            persistenceModule,
            externalModule,
            syncOverrideModule,
            usecaseModule,
            handlerModule,
            busModule,
            aggregatorModule
        )
    }

    val koin = koinApp.koin

    val dbConfig = koin.get<DatabaseConfig>()
    DatabaseFactory.init(dbConfig)

    runBlocking {
        val bus = koin.get<Bus>()

        logger.info("Starting aggregator sync...")
        bus(SyncAllActiveAggregatorCommand)
        logger.info("Aggregator sync completed")
    }
}
