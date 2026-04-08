package infrastructure.handler.collection

import application.IQueryHandler
import application.query.collection.BatchCollectionQuery
import domain.model.Collection
import infrastructure.persistence.entity.CollectionEntity
import infrastructure.persistence.mapper.CollectionMapper.toDomain
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.SortOrder
import infrastructure.persistence.dbRead

class BatchCollectionQueryHandler : IQueryHandler<BatchCollectionQuery, List<Collection>> {

    override suspend fun handle(query: BatchCollectionQuery): List<Collection> = dbRead {
        val identityValues = query.identities.map { it.value }

        CollectionEntity.find { CollectionTable.identity inList identityValues }
            .orderBy(CollectionTable.sortOrder to SortOrder.ASC)
            .map { it.toDomain() }
    }
}
