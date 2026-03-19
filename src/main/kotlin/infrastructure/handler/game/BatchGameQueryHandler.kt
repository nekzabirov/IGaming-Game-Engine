package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.BatchGameQuery
import domain.model.Game
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class BatchGameQueryHandler : IQueryHandler<BatchGameQuery, List<Game>> {

    override suspend fun handle(query: BatchGameQuery): List<Game> = newSuspendedTransaction {
        val identityValues = query.identities.map { it.value }

        val entities = GameEntity.find { GameTable.identity inList identityValues }
            .orderBy(GameTable.sortOrder to SortOrder.ASC)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val gameIds = entities.map { it.id }
        val integrations = entities.map { it.provider.aggregator.integration }.distinct()

        val variants = GameVariantEntity.find {
            (GameVariantTable.game inList gameIds) and
                    (GameVariantTable.integration inList integrations)
        }.toList()

        val variantMap = variants.associateBy { it.game.id.value to it.integration }

        entities.map { entity ->
            val game = entity.toDomain()
            val variantEntity = variantMap[entity.id.value to entity.provider.aggregator.integration]
            if (variantEntity != null) {
                game.variant = GameVariantMapper.run { variantEntity.toDomain() }
            }
            game
        }
    }
}
