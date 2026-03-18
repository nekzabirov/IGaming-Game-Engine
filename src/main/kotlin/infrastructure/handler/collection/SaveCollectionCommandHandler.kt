package infrastructure.handler.collection

import application.cqrs.ICommandHandler
import application.cqrs.collection.SaveCollectionCommand
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class SaveCollectionCommandHandler : ICommandHandler<SaveCollectionCommand, Unit> {
    override suspend fun handle(command: SaveCollectionCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            CollectionTable.upsert(keys = arrayOf(CollectionTable.identity)) {
                it[identity] = command.identity.value
                it[name] = command.name.data
                it[images] = emptyMap()
                it[active] = command.active
                it[sortOrder] = command.order
            }
        }
    }
}
