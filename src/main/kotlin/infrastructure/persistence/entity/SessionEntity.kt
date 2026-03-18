package infrastructure.persistence.entity

import infrastructure.persistence.table.SessionTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SessionEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SessionEntity>(SessionTable)

    var gameVariant by GameVariantEntity referencedOn SessionTable.gameVariant
    var playerId by SessionTable.playerId
    var token by SessionTable.token
    var externalToken by SessionTable.externalToken
    var currency by SessionTable.currency
    var locale by SessionTable.locale
    var platform by SessionTable.platform
}
