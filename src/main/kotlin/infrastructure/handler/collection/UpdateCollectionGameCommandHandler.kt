package infrastructure.handler.collection

import application.cqrs.ICommandHandler
import application.cqrs.collection.UpdateCollectionGameCommand
import domain.exception.notfound.CollectionNotFoundException
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UpdateCollectionGameCommandHandler : ICommandHandler<UpdateCollectionGameCommand, Unit> {
    override suspend fun handle(command: UpdateCollectionGameCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            val collectionRow = CollectionTable.selectAll()
                .where { CollectionTable.identity eq command.identity.value }
                .firstOrNull() ?: throw CollectionNotFoundException()

            val collectionId = collectionRow[CollectionTable.id]

            if (command.addGameIdentities.isNotEmpty()) {
                val gameIdentityValues = command.addGameIdentities.map { it.value }

                val gameRows = GameTable.selectAll()
                    .where { GameTable.identity inList gameIdentityValues }
                    .toList()

                val gameIds = gameRows.map { it[GameTable.id] }

                GameCollectionTable.batchInsert(gameIds, ignore = true) { gameId ->
                    this[GameCollectionTable.game] = gameId
                    this[GameCollectionTable.collection] = collectionId
                }
            }

            if (command.deleteGameIdentities.isNotEmpty()) {
                val deleteIdentityValues = command.deleteGameIdentities.map { it.value }

                val deleteGameRows = GameTable.selectAll()
                    .where { GameTable.identity inList deleteIdentityValues }
                    .toList()

                for (row in deleteGameRows) {
                    GameCollectionTable.deleteWhere {
                        (GameCollectionTable.game eq row[GameTable.id]) and
                                (GameCollectionTable.collection eq collectionId)
                    }
                }
            }
        }
    }
}
