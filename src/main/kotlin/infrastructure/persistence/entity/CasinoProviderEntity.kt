package infrastructure.persistence.entity

import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class CasinoProviderEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoProviderEntity>(CasinoProviderTable)

    var identity by CasinoProviderTable.identity
    var name by CasinoProviderTable.name
    var images by CasinoProviderTable.images
    var customImages by CasinoProviderTable.customImages
    var sortOrder by CasinoProviderTable.sortOrder
    var active by CasinoProviderTable.active
    var blockedCountry by CasinoProviderTable.blockedCountry
    var tags by CasinoProviderTable.tags
    var customTags by CasinoProviderTable.customTags
}
