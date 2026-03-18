package infrastructure.persistence.repository

import application.port.storage.ICollectionRepository
import domain.model.Collection
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.mapper.CollectionMapper.toCollection
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class CollectionRepositoryImpl : ICollectionRepository {

    override suspend fun save(collection: Collection): Collection = newSuspendedTransaction {
        CollectionTable.upsert(keys = arrayOf(CollectionTable.identity)) {
            it[identity] = collection.identity.value
            it[name] = collection.name.data
            it[images] = collection.images.data
            it[active] = collection.active
            it[sortOrder] = collection.order
        }

        collection
    }

    override suspend fun findByIdentity(identity: Identity): Collection? = newSuspendedTransaction {
        CollectionTable
            .selectAll()
            .where { CollectionTable.identity eq identity.value }
            .singleOrNull()
            ?.toCollection()
    }

    override suspend fun findAll(pageable: Pageable): Page<Collection> = newSuspendedTransaction {
        val totalItems = CollectionTable.selectAll().count()
        val items = CollectionTable
            .selectAll()
            .orderBy(CollectionTable.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toCollection() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
