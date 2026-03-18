package infrastructure.persistence.entity

import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class CollectionEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CollectionEntity>(CollectionTable)

    var identity by CollectionTable.identity
    var name by CollectionTable.name
    var images by CollectionTable.images
    var active by CollectionTable.active
    var sortOrder by CollectionTable.sortOrder
}
