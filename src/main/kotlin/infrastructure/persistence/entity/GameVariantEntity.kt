package infrastructure.persistence.entity

import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GameVariantEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GameVariantEntity>(GameVariantTable)

    var symbol by GameVariantTable.symbol
    var name by GameVariantTable.name
    var integration by GameVariantTable.integration
    var game by GameEntity referencedOn GameVariantTable.game
    var providerName by GameVariantTable.providerName
    var freeSpinEnable by GameVariantTable.freeSpinEnable
    var freeChipEnable by GameVariantTable.freeChipEnable
    var jackpotEnable by GameVariantTable.jackpotEnable
    var demoEnable by GameVariantTable.demoEnable
    var bonusBuyEnable by GameVariantTable.bonusBuyEnable
    var locales by GameVariantTable.locales
    var platforms by GameVariantTable.platforms
    var playLines by GameVariantTable.playLines
}
