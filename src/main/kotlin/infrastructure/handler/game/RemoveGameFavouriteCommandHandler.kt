package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.RemoveGameFavouriteCommand
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere

class RemoveGameFavouriteCommandHandler : ICommandHandler<RemoveGameFavouriteCommand, Unit> {

    override suspend fun handle(command: RemoveGameFavouriteCommand): Result<Unit> = runCatching {
        dbTransaction {
            val gameId = domainRequireNotNull(
                GameTable.select(GameTable.id)
                    .where { GameTable.identity eq command.identity.value }
                    .singleOrNull()?.get(GameTable.id)
            ) { GameNotFoundException() }

            GameFavouriteTable.deleteWhere {
                (game eq gameId) and (playerId eq command.playerId.value)
            }
        }
    }
}
