package infrastructure.persistence.entity

import infrastructure.persistence.table.CasinoRoundTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class CasinoRoundEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoRoundEntity>(CasinoRoundTable)

    var externalId by CasinoRoundTable.externalId
    var freespinId by CasinoRoundTable.freespinId
    var playerId by CasinoRoundTable.playerId
    var game by CasinoGameEntity optionalReferencedOn CasinoRoundTable.game
    var currency by CasinoRoundTable.currency
    var createdAt by CasinoRoundTable.createdAt
    var finishedAt by CasinoRoundTable.finishedAt
}
