package infrastructure.handler.aggregator

import application.cqrs.ICommandHandler
import application.cqrs.aggregator.SaveAggregatorCommand
import infrastructure.persistence.mapper.JsonMapperUtil.toJsonObject
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class SaveAggregatorCommandHandler : ICommandHandler<SaveAggregatorCommand, Unit> {
    override suspend fun handle(command: SaveAggregatorCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            AggregatorTable.upsert(keys = arrayOf(AggregatorTable.identity)) {
                it[identity] = command.identity.value
                it[config] = command.config.toJsonObject()
                it[active] = command.active
                it[integration] = command.integration
            }
        }
    }
}
