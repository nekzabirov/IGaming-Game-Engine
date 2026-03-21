package api.grpc

import api.grpc.service.AggregatorGrpcService
import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.GameGrpcService
import api.grpc.service.ProviderGrpcService
import api.grpc.service.WinnerGrpcService
import io.grpc.ServerBuilder
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
            .addService(get<GameGrpcService>())
            .addService(get<ProviderGrpcService>())
            .addService(get<CollectionGrpcService>())
            .addService(get<AggregatorGrpcService>())
            .addService(get<FreespinGrpcService>())
            .addService(get<WinnerGrpcService>())
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