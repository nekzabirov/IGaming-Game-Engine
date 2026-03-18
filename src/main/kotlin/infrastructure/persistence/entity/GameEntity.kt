package infrastructure.persistence.entity

import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GameEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GameEntity>(GameTable)

    var identity by GameTable.identity
    var name by GameTable.name
    var provider by ProviderEntity referencedOn GameTable.provider
    var bonusBetEnable by GameTable.bonusBetEnable
    var bonusWageringEnable by GameTable.bonusWageringEnable
    var tags by GameTable.tags
    var active by GameTable.active
    var images by GameTable.images
    var sortOrder by GameTable.sortOrder
    var collections by CollectionEntity via GameCollectionTable
}
