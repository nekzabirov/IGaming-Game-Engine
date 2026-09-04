package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllCasinoGameCollectionQuery
import domain.model.CasinoGame
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.search.searchPass
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.CasinoGameCollectionTable
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

class FindAllCasinoGameCollectionQueryHandler : IQueryHandler<FindAllCasinoGameCollectionQuery, Page<CasinoGame>> {

    override suspend fun handle(query: FindAllCasinoGameCollectionQuery): Page<CasinoGame> = dbRead {
        val collectionIdentity = query.collection.value

        // Phase 1 — page the join table ordered by per-collection sort order.
        fun rail(filterCondition: Op<Boolean>) =
            (CasinoGameCollectionTable innerJoin CasinoGameTable innerJoin CollectionTable)
                .select(CasinoGameTable.id, CasinoGameCollectionTable.sortOrder)
                .where {
                    (CollectionTable.identity eq collectionIdentity) and filterCondition
                }

        val pass = searchPass(
            relaxable = query.filter.isRelaxable(),
            condition = { relaxed -> query.filter.toCondition(relaxed) },
            count = { condition -> rail(condition).count() },
        )
        val baseQuery = rail(pass.condition)

        val totalItems = pass.totalItems
        val pageable = query.pageable

        val gameIds = baseQuery
            .orderBy(
                *query.filter.relevanceOrdering(),
                CasinoGameCollectionTable.sortOrder to SortOrder.ASC,
                CasinoGameTable.id to SortOrder.ASC,
            )
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[CasinoGameTable.id] }

        // Phase 2 — load the game entities in any order, then preserve the
        // page order in memory via a lookup map.
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
