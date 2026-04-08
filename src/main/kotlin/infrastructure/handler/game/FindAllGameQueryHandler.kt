package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllGameQuery
import application.query.game.GameView
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder

class FindAllGameQueryHandler : IQueryHandler<FindAllGameQuery, Page<GameView>> {

    override suspend fun handle(query: FindAllGameQuery): Page<GameView> = dbRead {
        val condition = query.filter.toCondition()
        val baseQuery = GameEntity.find { condition }
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
