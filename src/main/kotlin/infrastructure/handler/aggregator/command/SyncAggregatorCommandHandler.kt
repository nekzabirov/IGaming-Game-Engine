package com.nekgamebling.infrastructure.handler.aggregator.command

import application.port.inbound.CommandHandler
import application.service.GameSyncService
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorResponse
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SyncAggregatorCommandHandler(private val syncService: GameSyncService) :
    CommandHandler<SyncAllAggregatorCommand, SyncAllAggregatorResponse> {

    override suspend fun handle(command: SyncAllAggregatorCommand): Result<SyncAllAggregatorResponse> = runCatching {
        val aggregators = newSuspendedTransaction {
            AggregatorInfoTable.selectAll().where { AggregatorInfoTable.active eq true }.toList()
        }

        println("Aggregators: ${aggregators.size}")

        var total = 0

        for (row in aggregators) {
            val result = syncService.sync(row[AggregatorInfoTable.identity]).getOrThrow()

            total += result.gameCount
        }

        return Result.success(SyncAllAggregatorResponse(total))
    }
}