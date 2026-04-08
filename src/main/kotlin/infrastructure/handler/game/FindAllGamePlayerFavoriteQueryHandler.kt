package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllGamePlayerFavoriteQuery
import application.query.game.GameView
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

class FindAllGamePlayerFavoriteQueryHandler : IQueryHandler<FindAllGamePlayerFavoriteQuery, Page<GameView>> {

    override suspend fun handle(query: FindAllGamePlayerFavoriteQuery): Page<GameView> = dbRead {
        val filterCondition = query.filter.toCondition()

        val baseQuery = (GameFavouriteTable innerJoin GameTable)
            .select(GameTable.id, GameFavouriteTable.id)
            .where {
                (GameFavouriteTable.playerId eq query.playerId.value) and filterCondition
            }

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val gameIds = baseQuery
            .orderBy(GameFavouriteTable.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[GameTable.id] }

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
