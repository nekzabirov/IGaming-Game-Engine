package com.nekgamebling

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorResponse
import infrastructure.coreModule
import infrastructure.persistence.exposed.ExposedConfig
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.logger.slf4jLogger
import kotlin.system.exitProcess

/**
 * Standalone CLI entry point for syncing all aggregators.
 * Initializes Koin, runs the sync command, and exits.
 */
fun main() {
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    // Initialize database
    ExposedConfig.init()

    // Start Koin with minimal modules needed for sync
    val koinApp = startKoin {
        slf4jLogger()
        modules(coreModule())
    }

    val koin = koinApp.koin

    try {
        runBlocking {
            val handler = koin.get<CommandHandler<SyncAllAggregatorCommand, SyncAllAggregatorResponse>>(
                named("syncAllAggregators")
            )

            println("Starting aggregator sync...")

            val result = handler.handle(SyncAllAggregatorCommand)

            result.fold(
                onSuccess = { response ->
                    println("Sync completed successfully. Total games synced: ${response.totalGames}")
                },
                onFailure = { error ->
                    System.err.println("Sync failed: ${error.message}")
                    error.printStackTrace()
                    stopKoin()
                    exitProcess(1)
                }
            )
        }
    } finally {
        stopKoin()
    }

    exitProcess(0)
}