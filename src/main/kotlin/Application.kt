import clients.GameHubClient
import clients.PamWallet
import clients.RedisPlayerLimits
import events.RabbitEventPublisher
import events.rabbitConnection
import grpc.CasinoGameGrpcService
import grpc.CasinoProviderGrpcService
import grpc.CasinoRoundGrpcService
import grpc.CollectionGrpcService
import grpc.FreespinGrpcService
import grpc.JackpotGrpcService
import grpc.SportbookGrpcService
import grpc.WalletGrpcService
import grpc.WinnerGrpcService
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import plugins.configureGrpc
import plugins.configureMessaging
import plugins.configureRouting
import plugins.connectDatabase
import services.CollectionService
import services.GameService
import services.LaunchService
import services.ProviderService
import services.RoundService
import services.WalletService
import services.WinnerService
import java.util.TimeZone

fun main() {
    // grpc-netty-shaded prefers its bundled BoringSSL for TLS, and loading it in this image kills
    // the JVM outright (SIGSEGV in netty_internal_tcnative_SSLContext_JNI_OnLoad). The flag has to
    // be set before any Netty SSL class loads.
    System.setProperty("io.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl", "true")

    System.setProperty("user.timezone", "UTC")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    val config = AppConfig.fromEnv()

    embeddedServer(CIO, port = config.httpPort, host = "0.0.0.0") { module(config) }.start(wait = true)
}

/** Explicit wiring: every dependency is built once, here, and handed to what needs it. */
fun Application.module(config: AppConfig) {
    connectDatabase(config.database)

    val gameHub = GameHubClient(config.gameHub)
    val wallet = PamWallet(config.pam)
    val limits = RedisPlayerLimits(config.redis)
    val rabbit = rabbitConnection(config.rabbit)
    val events = RabbitEventPublisher(rabbit, config.eventExchange)

    val games = GameService()
    val providers = ProviderService()
    val collections = CollectionService()
    val rounds = RoundService()
    val winners = WinnerService()
    val launch = LaunchService(gameHub, limits, events)
    val walletService = WalletService(wallet, limits, events, config.freespinToPayout)

    configureMessaging(rabbit, config.eventExchange, limits)
    configureGrpc(
        port = config.grpcPort,
        services = listOf(
            CasinoGameGrpcService(games, launch),
            CasinoProviderGrpcService(providers),
            CollectionGrpcService(collections),
            CasinoRoundGrpcService(rounds),
            WinnerGrpcService(winners),
            FreespinGrpcService(launch),
            SportbookGrpcService(launch),
            JackpotGrpcService(),
        ),
        wallet = WalletGrpcService(walletService, config.gameHub),
    )
    configureRouting()
}
