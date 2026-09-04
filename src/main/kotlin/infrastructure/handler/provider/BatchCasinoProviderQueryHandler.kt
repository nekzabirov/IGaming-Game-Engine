package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.BatchCasinoProviderQuery
import domain.model.CasinoProvider
import infrastructure.persistence.entity.CasinoProviderEntity
import infrastructure.persistence.mapper.CasinoProviderMapper.toDomain
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.SortOrder
import infrastructure.persistence.dbRead

class BatchCasinoProviderQueryHandler : IQueryHandler<BatchCasinoProviderQuery, List<CasinoProvider>> {

    override suspend fun handle(query: BatchCasinoProviderQuery): List<CasinoProvider> = dbRead {
        val identityValues = query.identities.map { it.value }

        CasinoProviderEntity.find { CasinoProviderTable.identity inList identityValues }
            .orderBy(CasinoProviderTable.sortOrder to SortOrder.ASC)
            .map { it.toDomain() }
    }
}
