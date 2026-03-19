package infrastructure.handler.collection

import application.cqrs.IQueryHandler
import application.cqrs.collection.BatchCollectionQuery
import domain.model.Collection
import infrastructure.persistence.entity.CollectionEntity
import infrastructure.persistence.mapper.CollectionMapper.toDomain
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class BatchCollectionQueryHandler : IQueryHandler<BatchCollectionQuery, List<Collection>> {

    override suspend fun handle(query: BatchCollectionQuery): List<Collection> = newSuspendedTransaction {
        val identityValues = query.identities.map { it.value }

        CollectionEntity.find { CollectionTable.identity inList identityValues }
            .orderBy(CollectionTable.sortOrder to SortOrder.ASC)
            .map { it.toDomain() }
    }
}
