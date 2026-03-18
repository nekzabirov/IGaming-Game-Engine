package infrastructure.koin

import api.grpc.config.grpcModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val application = this
    install(Koin) {
        slf4jLogger()
        modules(
            module { single { application } },
            configModule,
            persistenceModule,
            externalModule,
            usecaseModule,
            handlerModule,
            busModule,
            aggregatorModule,
            grpcModule
        )
    }
}
