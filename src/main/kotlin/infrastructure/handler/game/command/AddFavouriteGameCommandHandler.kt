package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.AddFavouriteGameCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameFavouriteTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddFavouriteGameCommandHandler : CommandHandler<AddFavouriteGameCommand, Unit> {
    override suspend fun handle(command: AddFavouriteGameCommand): Result<Unit> = newSuspendedTransaction {
        val game = GameTable
            .selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        val gameId = game[GameTable.id]

        val exists = GameFavouriteTable
            .selectAll()
            .where { (GameFavouriteTable.playerId eq command.playerId) and (GameFavouriteTable.gameId eq gameId) }
            .firstOrNull() != null

        if (!exists) {
            GameFavouriteTable.insert {
                it[playerId] = command.playerId
                it[this.gameId] = gameId
            }
        }

        Result.success(Unit)
    }
}
