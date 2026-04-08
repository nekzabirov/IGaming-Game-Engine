package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.AddGameFavouriteCommand
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.insertIgnore

class AddGameFavouriteCommandHandler : ICommandHandler<AddGameFavouriteCommand, Unit> {

    override suspend fun handle(command: AddGameFavouriteCommand): Result<Unit> = runCatching {
        dbTransaction {
            val gameId = domainRequireNotNull(
                GameTable.select(GameTable.id)
                    .where { GameTable.identity eq command.identity.value }
                    .singleOrNull()?.get(GameTable.id)
            ) { GameNotFoundException() }

            GameFavouriteTable.insertIgnore {
                it[game] = gameId
                it[playerId] = command.playerId.value
            }
        }
    }
}
