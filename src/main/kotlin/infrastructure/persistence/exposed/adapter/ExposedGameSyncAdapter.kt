package infrastructure.persistence.exposed.adapter

import application.port.outbound.GameSyncAdapter
import domain.aggregator.AggregatorInfo
import domain.game.model.GameVariant
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameVariantTable
import infrastructure.persistence.exposed.table.ProviderTable
import shared.extension.toUrlSlug
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning

/**
 * Exposed implementation of GameSyncAdapter.
 * Handles complex sync operations within a single transaction.
 */
class ExposedGameSyncAdapter : GameSyncAdapter {
    override suspend fun syncGame(variants: List<GameVariant>, aggregatorInfo: AggregatorInfo) =
        newSuspendedTransaction {
            for (gameVariant in variants) {
                if (gameVariant.gameId != null) continue

                val providerIdentity = gameVariant.providerName.toUrlSlug()

                val provider = ProviderTable.upsertReturning(
                    keys = arrayOf(ProviderTable.identity),
                    onUpdateExclude = listOf(
                        ProviderTable.id,
                        ProviderTable.name,
                        ProviderTable.createdAt,
                        ProviderTable.name
                    ),
                ) {
                    it[identity] = providerIdentity
                    it[name] = gameVariant.providerName
                    it[aggregatorId] = aggregatorInfo.id
                }.single().toProvider()

                val gameIdentity = "${provider.identity}-${gameVariant.name}".toUrlSlug()

                var gameId = GameTable.select(GameTable.id)
                    .where { ((GameTable.identity eq gameIdentity) or (GameTable.name eq gameVariant.name)) and (GameTable.providerId eq provider.id) }
                    .singleOrNull()?.get(GameTable.id)?.value

                if (gameId == null) {
                    gameId = GameTable.insertAndGetId {
                        it[identity] = gameIdentity
                        it[name] = gameVariant.name
                        it[providerId] = provider.id
                    }.value
                }

                GameVariantTable.update(where = { GameVariantTable.id eq gameVariant.id }) {
                    it[GameVariantTable.gameId] = gameId
                }
            }
        }
}
