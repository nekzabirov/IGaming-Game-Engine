package api.grpc.config

import infrastructure.koin.aggregatorModule
import infrastructure.koin.busModule
import infrastructure.koin.configModule
import infrastructure.koin.externalModule
import infrastructure.koin.handlerModule
import infrastructure.koin.persistenceModule
import infrastructure.koin.usecaseModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * Composition root.
 *
 * Lives in the `api` layer because it is the only layer that knows about every other
 * layer (domain, application, infrastructure) — wiring DI is the outermost concern,
 * not an infrastructure responsibility. Keeping it here removes the inverted import
 * (`infrastructure → api`) the previous setup had.
 */
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
            grpcModule,
        )
    }
}
