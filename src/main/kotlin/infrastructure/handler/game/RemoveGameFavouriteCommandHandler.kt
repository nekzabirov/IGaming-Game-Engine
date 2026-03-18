package infrastructure.handler.game

import application.cqrs.ICommandHandler
import application.cqrs.game.RemoveGameFavouriteCommand
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RemoveGameFavouriteCommandHandler : ICommandHandler<RemoveGameFavouriteCommand, Unit> {
    override suspend fun handle(command: RemoveGameFavouriteCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            val gameId = GameTable.select(GameTable.id)
                .where { GameTable.identity eq command.identity.value }
                .singleOrNull()?.get(GameTable.id)
                ?: throw IllegalArgumentException("Game not found: ${command.identity.value}")

            GameFavouriteTable.deleteWhere {
                (game eq gameId) and
                        (playerId eq command.playerId.value)
            }
        }
    }
}
