package infrastructure.handler.game

import application.cqrs.ICommandHandler
import application.cqrs.game.AddGameFavouriteCommand
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddGameFavouriteCommandHandler : ICommandHandler<AddGameFavouriteCommand, Unit> {
    override suspend fun handle(command: AddGameFavouriteCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            val gameId = GameTable.select(GameTable.id)
                .where { GameTable.identity eq command.identity.value }
                .singleOrNull()?.get(GameTable.id)
                ?: throw IllegalArgumentException("Game not found: ${command.identity.value}")

            GameFavouriteTable.insertIgnore {
                it[game] = gameId
                it[playerId] = command.playerId.value
            }
        }
    }
}
