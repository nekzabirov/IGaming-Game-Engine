package infrastructure.persistence.entity

import infrastructure.persistence.table.CasinoGameCollectionTable
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class CasinoGameEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoGameEntity>(CasinoGameTable)

    var identity by CasinoGameTable.identity
    var name by CasinoGameTable.name
    var provider by CasinoProviderEntity referencedOn CasinoGameTable.provider
    var bonusBetEnable by CasinoGameTable.bonusBetEnable
    var bonusWageringEnable by CasinoGameTable.bonusWageringEnable
    var tags by CasinoGameTable.tags
    var rtp by CasinoGameTable.rtp
    var freeSpinEnable by CasinoGameTable.freeSpinEnable
    var freeChipEnable by CasinoGameTable.freeChipEnable
    var jackpotEnable by CasinoGameTable.jackpotEnable
    var demoEnable by CasinoGameTable.demoEnable
    var bonusBuyEnable by CasinoGameTable.bonusBuyEnable
    var locales by CasinoGameTable.locales
    var platforms by CasinoGameTable.platforms
    var playLines by CasinoGameTable.playLines
    var active by CasinoGameTable.active
    var images by CasinoGameTable.images
    var customImages by CasinoGameTable.customImages
    var sortOrder by CasinoGameTable.sortOrder
    var collections by CollectionEntity via CasinoGameCollectionTable
}
