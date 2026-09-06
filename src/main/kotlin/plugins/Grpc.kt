package plugins

import grpc.HubCredentialsInterceptor
import grpc.WalletGrpcService
import io.grpc.BindableService
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("grpc")

/**
 * The gRPC server next to the Ktor one. [wallet] is registered behind the hub credential
 * interceptor: the hub authenticates every money call with headers a coroutine service cannot read
 * by itself. Stops with the application.
 */
fun Application.configureGrpc(port: Int, services: List<BindableService>, wallet: WalletGrpcService) {
    val server = ServerBuilder.forPort(port)
        .apply { services.forEach { addService(it) } }
        .addService(ServerInterceptors.intercept(wallet, HubCredentialsInterceptor))
        .build()
        .start()

    log.info("gRPC server started on port {}", port)

    monitor.subscribe(ApplicationStopping) {
        server.shutdown()
        server.awaitTermination(10, TimeUnit.SECONDS)
    }
}
