package api.rest

import infrastructure.aggregator.onegamehub.webhook.OneGameHubWebhook
import infrastructure.aggregator.pragmatic.webhook.PragmaticWebhook
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    val oneGameHubWebhook = get<OneGameHubWebhook>()
    val pragmaticWebhook = get<PragmaticWebhook>()

    routing {
        route("/api/webhook") {
            with(oneGameHubWebhook) { route() }
            with(pragmaticWebhook) { route() }
        }
    }
}

fun httpPort(): Int = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080
