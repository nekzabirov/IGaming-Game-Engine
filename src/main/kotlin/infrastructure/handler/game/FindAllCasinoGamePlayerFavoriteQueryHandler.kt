package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllCasinoGamePlayerFavoriteQuery
import domain.model.CasinoGame
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.search.searchPass
import infrastructure.persistence.table.CasinoGameFavouriteTable
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

class FindAllCasinoGamePlayerFavoriteQueryHandler : IQueryHandler<FindAllCasinoGamePlayerFavoriteQuery, Page<CasinoGame>> {

    override suspend fun handle(query: FindAllCasinoGamePlayerFavoriteQuery): Page<CasinoGame> = dbRead {
        fun favourites(filterCondition: Op<Boolean>) =
            (CasinoGameFavouriteTable innerJoin CasinoGameTable)
                .select(CasinoGameTable.id, CasinoGameFavouriteTable.id)
                .where {
                    (CasinoGameFavouriteTable.playerId eq query.playerId.value) and filterCondition
                }

        val pass = searchPass(
            relaxable = query.filter.isRelaxable(),
            condition = { relaxed -> query.filter.toCondition(relaxed) },
            count = { condition -> favourites(condition).count() },
        )
        val baseQuery = favourites(pass.condition)

        val totalItems = pass.totalItems
        val pageable = query.pageable

        val gameIds = baseQuery
            .orderBy(
                *query.filter.relevanceOrdering(),
                CasinoGameFavouriteTable.id to SortOrder.DESC,
            )
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[CasinoGameTable.id] }

        val entities = CasinoGameEntity.forEntityIds(gameIds)
            .with(CasinoGameEntity::provider, CasinoGameEntity::collections)
            .toList()

        val gamesById = entities.associate { it.id to it.toDomain() }

        val items = gameIds.mapNotNull { id -> gamesById[id] }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
