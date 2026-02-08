package com.nekgamebling.infrastructure.handler.aggregator.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorResponse
import domain.aggregator.AggregatorInfo
import domain.common.error.DuplicateEntityError
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class CreateAggregatorCommandHandler : CommandHandler<CreateAggregatorCommand, CreateAggregatorResponse> {

    override suspend fun handle(command: CreateAggregatorCommand): Result<CreateAggregatorResponse> = newSuspendedTransaction {
        // Check if identity already exists
        val exists = AggregatorInfoTable
            .selectAll()
            .where { AggregatorInfoTable.identity eq command.identity }
            .count() > 0

        if (exists) {
            return@newSuspendedTransaction Result.failure(
                DuplicateEntityError("Aggregator", command.identity)
            )
        }

        // Insert new aggregator
        val id = AggregatorInfoTable.insertAndGetId {
            it[identity] = command.identity
            it[aggregator] = command.aggregator
            it[config] = command.config
            it[active] = command.active
        }

        val aggregatorInfo = AggregatorInfo(
            id = id.value,
            identity = command.identity,
            aggregator = command.aggregator,
            config = command.config,
            active = command.active
        )

        Result.success(CreateAggregatorResponse(aggregator = aggregatorInfo))
    }
}
