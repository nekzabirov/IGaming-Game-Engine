package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.AddGameWonCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameWonTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddGameWonCommandHandler : CommandHandler<AddGameWonCommand, Unit> {
    override suspend fun handle(command: AddGameWonCommand): Result<Unit> = newSuspendedTransaction {
        val game = GameTable
            .selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        val gameId = game[GameTable.id]

        GameWonTable.insert {
            it[this.gameId] = gameId
            it[playerId] = command.playerId
            it[amount] = command.amount
            it[currency] = command.currency
        }

        Result.success(Unit)
    }
}
