package infrastructure.handler.provider

import application.cqrs.ICommandHandler
import application.cqrs.provider.SaveProviderCommand
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class SaveProviderCommandHandler : ICommandHandler<SaveProviderCommand, Unit> {
    override suspend fun handle(command: SaveProviderCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            val aggregatorId = AggregatorTable.select(AggregatorTable.id)
                .where { AggregatorTable.identity eq command.aggregatorIdentity.value }
                .singleOrNull()?.get(AggregatorTable.id)
                ?: throw IllegalArgumentException("Aggregator not found: ${command.aggregatorIdentity.value}")

            ProviderTable.upsert(keys = arrayOf(ProviderTable.identity)) {
                it[identity] = command.identity.value
                it[name] = command.name
                it[sortOrder] = command.order
                it[active] = command.active
                it[aggregator] = aggregatorId
            }
        }
    }
}
