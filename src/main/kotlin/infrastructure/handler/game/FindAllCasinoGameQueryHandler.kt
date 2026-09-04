package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllCasinoGameQuery
import domain.model.CasinoGame
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.search.searchPass
import org.jetbrains.exposed.dao.with

class FindAllCasinoGameQueryHandler : IQueryHandler<FindAllCasinoGameQuery, Page<CasinoGame>> {

    override suspend fun handle(query: FindAllCasinoGameQuery): Page<CasinoGame> = dbRead {
        val filter = query.filter
        val pass = searchPass(
            relaxable = filter.isRelaxable(),
            condition = { relaxed -> filter.toCondition(relaxed) },
            count = { condition -> CasinoGameEntity.find { condition }.count() },
        )
        val baseQuery = CasinoGameEntity.find { pass.condition }
        val totalItems = pass.totalItems
        val pageable = query.pageable

        val items = baseQuery
            .orderBy(*filter.toOrdering())
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(CasinoGameEntity::provider, CasinoGameEntity::collections)
            .map { it.toDomain() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
