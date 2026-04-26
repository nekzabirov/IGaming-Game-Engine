import application.Bus
import application.command.aggregator.SyncAllActiveAggregatorCommand
import application.port.external.IEventPort
import domain.event.DomainEvent
import infrastructure.koin.aggregatorModule
import infrastructure.koin.busModule
import infrastructure.koin.configModule
import infrastructure.koin.externalModule
import infrastructure.koin.handlerModule
import infrastructure.koin.persistenceModule
import infrastructure.koin.usecaseModule
import infrastructure.persistence.DatabaseConfig
import infrastructure.persistence.DatabaseFactory
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.nekgamebling.SyncJob")

private val syncOverrideModule = module {
    single<IEventPort> {
        object : IEventPort {
            override suspend fun publish(event: DomainEvent) {
                // no-op: SyncJob does not publish events
            }
        }
    }
}

fun main() {
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
