package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.BatchGameQuery
import application.query.game.GameView
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder

class BatchGameQueryHandler : IQueryHandler<BatchGameQuery, List<GameView>> {

    override suspend fun handle(query: BatchGameQuery): List<GameView> = dbRead {
        val identityValues = query.identities.map { it.value }

        val entities = GameEntity.find { GameTable.identity inList identityValues }
            .orderBy(GameTable.sortOrder to SortOrder.ASC)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val variantMap = entities.loadVariantMap()

        entities.map { entity ->
            GameView(
                game = entity.toDomain(),
                variant = entity.variantFrom(variantMap)?.toDomain(),
            )
        }
    }
}
