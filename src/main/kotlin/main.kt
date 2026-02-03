package com.nekgamebling

import com.nekgamebling.infrastructure.api.installApi
import infrastructure.coreModule
import infrastructure.messaging.messagingModule
import infrastructure.persistence.exposed.ExposedConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(
        CIO,
        port = System.getenv("HTTP_PORT").toIntOrNull() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    ExposedConfig.init()

    install(Koin) {
        slf4jLogger()
        modules(coreModule())
        modules(messagingModule(this@module))
    }

    install(ContentNegotiation) {
        json()
    }

    installApi()
}
