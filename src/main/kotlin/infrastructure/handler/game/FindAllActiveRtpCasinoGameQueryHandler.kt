package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindAllActiveRtpCasinoGameQuery
import application.query.game.CasinoGameRtpType
import domain.model.CasinoGame
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.search.searchPass
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

class FindAllActiveRtpCasinoGameQueryHandler : IQueryHandler<FindAllActiveRtpCasinoGameQuery, Page<CasinoGame>> {

    override suspend fun handle(query: FindAllActiveRtpCasinoGameQuery): Page<CasinoGame> = dbRead {
        val rtpCondition = when (query.type) {
            CasinoGameRtpType.HOT -> Op.build { CasinoGameTable.rtp greater CasinoGame.DEFAULT_RTP }
            CasinoGameRtpType.COLD -> Op.build { CasinoGameTable.rtp less CasinoGame.DEFAULT_RTP }
        }
        val rtpOrder = when (query.type) {
            CasinoGameRtpType.HOT -> SortOrder.DESC
            CasinoGameRtpType.COLD -> SortOrder.ASC
        }

        val pass = searchPass(
            relaxable = query.filter.isRelaxable(),
            condition = { relaxed ->
                query.filter.toCondition(relaxed) and Op.build { CasinoGameTable.active eq true } and rtpCondition
            },
            count = { condition -> CasinoGameEntity.find { condition }.count() },
        )
        val baseQuery = CasinoGameEntity.find { pass.condition }
        val totalItems = pass.totalItems
        val pageable = query.pageable

        // id tiebreaker: same reason as CasinoGameFilter.toOrdering — equal (rtp, sortOrder)
        // keys give unstable pagination.
        val items = baseQuery
            .orderBy(
                CasinoGameTable.rtp to rtpOrder,
                CasinoGameTable.sortOrder to SortOrder.ASC,
                CasinoGameTable.id to SortOrder.ASC,
            )
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
