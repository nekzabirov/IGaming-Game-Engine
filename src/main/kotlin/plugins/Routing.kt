package plugins

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/** The HTTP side serves no API — gRPC does — only a liveness answer. */
fun Application.configureRouting() {
    routing {
        get("/health") { call.respondText("ok") }
    }
}
