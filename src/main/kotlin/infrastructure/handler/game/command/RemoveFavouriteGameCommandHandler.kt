package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.RemoveFavouriteGameCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameFavouriteTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RemoveFavouriteGameCommandHandler : CommandHandler<RemoveFavouriteGameCommand, Unit> {
    override suspend fun handle(command: RemoveFavouriteGameCommand): Result<Unit> = newSuspendedTransaction {
        val game = GameTable
            .selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        val gameId = game[GameTable.id]

        GameFavouriteTable.deleteWhere {
            (GameFavouriteTable.playerId eq command.playerId) and (GameFavouriteTable.gameId eq gameId)
        }

        Result.success(Unit)
    }
}
