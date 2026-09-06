import clients.GameHubClient
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import plugins.connectDatabase
import services.CatalogSync
import java.util.TimeZone
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("SyncJob")

/** One-shot: pulls the whole catalog from the hub into the database, then exits. Publishes nothing. */
fun main() {
    System.setProperty("io.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl", "true")
    System.setProperty("user.timezone", "UTC")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    val config = AppConfig.fromEnv()
    connectDatabase(config.database)
    val gameHub = GameHubClient(config.gameHub)

    try {
        log.info("Starting catalog sync...")
        runBlocking { CatalogSync(gameHub).run() }
        log.info("Catalog sync completed")
    } catch (e: Exception) {
        log.error("Catalog sync failed", e)
        exitProcess(1)
    } finally {
        gameHub.close()
    }

    exitProcess(0)
}
