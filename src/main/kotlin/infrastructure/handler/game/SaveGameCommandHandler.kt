package infrastructure.handler.game

import application.cqrs.ICommandHandler
import application.cqrs.game.SaveGameCommand
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class SaveGameCommandHandler : ICommandHandler<SaveGameCommand, Unit> {
    override suspend fun handle(command: SaveGameCommand): Result<Unit> = runCatching {
        newSuspendedTransaction {
            val providerId = ProviderTable.select(ProviderTable.id)
                .where { ProviderTable.identity eq command.providerIdentity.value }
                .singleOrNull()?.get(ProviderTable.id)
                ?: throw IllegalArgumentException("Provider not found: ${command.providerIdentity.value}")

            GameTable.upsert(keys = arrayOf(GameTable.identity)) {
                it[identity] = command.identity.value
                it[name] = command.name
                it[bonusBetEnable] = command.bonusBetEnable
                it[bonusWageringEnable] = command.bonusWageringEnable
                it[tags] = command.tags
                it[provider] = providerId
            }
        }
    }
}
