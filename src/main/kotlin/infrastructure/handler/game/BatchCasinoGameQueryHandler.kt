package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.BatchCasinoGameQuery
import domain.model.CasinoGame
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder

class BatchCasinoGameQueryHandler : IQueryHandler<BatchCasinoGameQuery, List<CasinoGame>> {

    override suspend fun handle(query: BatchCasinoGameQuery): List<CasinoGame> = dbRead {
        val identityValues = query.identities.map { it.value }

        CasinoGameEntity.find { CasinoGameTable.identity inList identityValues }
            .orderBy(CasinoGameTable.sortOrder to SortOrder.ASC)
            .with(CasinoGameEntity::provider, CasinoGameEntity::collections)
            .map { it.toDomain() }
    }
}
