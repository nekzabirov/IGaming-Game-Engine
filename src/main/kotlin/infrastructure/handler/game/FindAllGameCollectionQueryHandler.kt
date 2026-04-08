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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists

class FindAllGameCollectionQueryHandler : IQueryHandler<FindAllGameCollectionQuery, Page<GameView>> {

    override suspend fun handle(query: FindAllGameCollectionQuery): Page<GameView> = dbRead {
        val filterCondition = query.filter.toCondition()
        val collectionIdentity = query.collection.value

        val membership = exists(
            GameCollectionTable
                .innerJoin(CollectionTable)
                .select(GameCollectionTable.game)
                .where {
                    (GameCollectionTable.game eq GameTable.id) and
                            (CollectionTable.identity eq collectionIdentity)
                }
        )

        val baseQuery = GameEntity.find { filterCondition and membership }
        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val entities = baseQuery
            .orderBy(GameTable.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val variantMap = entities.loadVariantMap()

        val items = entities.map { entity ->
            GameView(
                game = entity.toDomain(),
                variant = entity.variantFrom(variantMap)?.toDomain(),
            )
        }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
