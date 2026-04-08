package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllGameCollectionQuery
import application.query.game.GameView
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

class FindAllGameCollectionQueryHandler : IQueryHandler<FindAllGameCollectionQuery, Page<GameView>> {

    override suspend fun handle(query: FindAllGameCollectionQuery): Page<GameView> = dbRead {
        val filterCondition = query.filter.toCondition()
        val collectionIdentity = query.collection.value

        // Phase 1 — page the join table ordered by per-collection sort order.
        val baseQuery = (GameCollectionTable innerJoin GameTable innerJoin CollectionTable)
            .select(GameTable.id, GameCollectionTable.sortOrder)
            .where {
                (CollectionTable.identity eq collectionIdentity) and filterCondition
            }

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val gameIds = baseQuery
            .orderBy(
                GameCollectionTable.sortOrder to SortOrder.ASC,
                GameTable.id to SortOrder.ASC,
            )
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[GameTable.id] }

        // Phase 2 — load the game entities in any order, then preserve the
        // page order in memory via a lookup map.
        val entities = GameEntity.forEntityIds(gameIds)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val variantMap = entities.loadVariantMap()

        val viewsById = entities.associate { entity ->
            entity.id to GameView(
                game = entity.toDomain(),
                variant = entity.variantFrom(variantMap)?.toDomain(),
            )
        }

        val items = gameIds.mapNotNull { id -> viewsById[id] }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
