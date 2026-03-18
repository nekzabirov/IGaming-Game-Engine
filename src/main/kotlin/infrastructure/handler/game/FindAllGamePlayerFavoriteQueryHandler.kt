package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.FindAllGamePlayerFavoriteQuery
import domain.model.Game
import domain.vo.Page
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.table.GameFavouriteTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllGamePlayerFavoriteQueryHandler : IQueryHandler<FindAllGamePlayerFavoriteQuery, Page<Game>> {

    override suspend fun handle(query: FindAllGamePlayerFavoriteQuery): Page<Game> = newSuspendedTransaction {
        val totalItems = GameFavouriteTable
            .selectAll()
            .where { GameFavouriteTable.playerId eq query.playerId.value }
            .count()

        val pageable = query.pageable

        val gameIds = GameFavouriteTable
            .select(GameFavouriteTable.game)
            .where { GameFavouriteTable.playerId eq query.playerId.value }
            .orderBy(GameFavouriteTable.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[GameFavouriteTable.game] }

        val gamesById = GameEntity.forEntityIds(gameIds)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()
            .associateBy { it.id }

        val items = gameIds.mapNotNull { id -> gamesById[id]?.toDomain() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
