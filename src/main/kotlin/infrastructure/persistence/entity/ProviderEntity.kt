package infrastructure.persistence.entity

import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ProviderEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ProviderEntity>(ProviderTable)

    var identity by ProviderTable.identity
    var name by ProviderTable.name
    var images by ProviderTable.images
    var sortOrder by ProviderTable.sortOrder
    var active by ProviderTable.active
    var aggregator by AggregatorEntity referencedOn ProviderTable.aggregator
}
