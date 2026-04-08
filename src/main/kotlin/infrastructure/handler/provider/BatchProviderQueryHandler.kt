package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.BatchProviderQuery
import domain.model.Provider
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.ProviderMapper.toDomain
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import infrastructure.persistence.dbRead

class BatchProviderQueryHandler : IQueryHandler<BatchProviderQuery, List<Provider>> {

    override suspend fun handle(query: BatchProviderQuery): List<Provider> = dbRead {
        val identityValues = query.identities.map { it.value }

        ProviderEntity.find { ProviderTable.identity inList identityValues }
            .orderBy(ProviderTable.sortOrder to SortOrder.ASC)
            .with(ProviderEntity::aggregator)
            .map { it.toDomain() }
    }
}
