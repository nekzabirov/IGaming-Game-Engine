package api.grpc

import api.grpc.service.AggregatorGrpcService
import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.CasinoGameGrpcService
import api.grpc.service.JackpotGrpcService
import api.grpc.service.OperatorCredentialsInterceptor
import api.grpc.service.OperatorWalletGrpcService
import api.grpc.service.CasinoProviderGrpcService
import api.grpc.service.SportbookGrpcService
import api.grpc.service.WinnerGrpcService
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.nekgamebling.Main")

fun Application.configureGrpc() {
    val grpcPort = grpcPort()

    launch(Dispatchers.IO) {
        val server = ServerBuilder.forPort(grpcPort)
            .addService(get<CasinoGameGrpcService>())
            .addService(get<CasinoProviderGrpcService>())
            .addService(get<CollectionGrpcService>())
            .addService(get<AggregatorGrpcService>())
            .addService(get<FreespinGrpcService>())
            .addService(get<WinnerGrpcService>())
            .addService(get<JackpotGrpcService>())
            .addService(get<SportbookGrpcService>())
            // GameHub calls us for every money movement, so its credential headers have to
            // reach the call: a CoroutineImplBase method cannot read metadata by itself.
            .addService(
                ServerInterceptors.intercept(get<OperatorWalletGrpcService>(), OperatorCredentialsInterceptor)
            )
            .build()
            .start()

        logger.info("gRPC server started on port $grpcPort")

        Runtime.getRuntime().addShutdownHook(Thread {
            server.shutdown()
        })

        server.awaitTermination()
    }
}

private fun grpcPort(): Int = System.getenv("GRPC_PORT")?.toIntOrNull() ?: 5050