package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllCasinoGamePlayerLastQuery
import domain.model.CasinoGame
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoRoundTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.dao.with

class FindAllCasinoGamePlayerLastQueryHandler : IQueryHandler<FindAllCasinoGamePlayerLastQuery, Page<CasinoGame>> {

    override suspend fun handle(query: FindAllCasinoGamePlayerLastQuery): Page<CasinoGame> = dbRead {
        // The round's own PK is the recency order — a sportsbook leg (game == null) is excluded.
        val lastRoundId = CasinoRoundTable.id.max()

        val baseQuery = CasinoRoundTable
            .select(CasinoRoundTable.game, lastRoundId)
            .where { (CasinoRoundTable.playerId eq query.playerId.value) and CasinoRoundTable.game.isNotNull() }
            .groupBy(CasinoRoundTable.game)

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val gameIds = baseQuery
            .orderBy(lastRoundId to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .mapNotNull { it[CasinoRoundTable.game] }

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
