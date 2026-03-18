package infrastructure.persistence.entity

import infrastructure.persistence.table.RoundTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class RoundEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RoundEntity>(RoundTable)

    var externalId by RoundTable.externalId
    var freespinId by RoundTable.freespinId
    var session by SessionEntity referencedOn RoundTable.session
    var gameVariant by GameVariantEntity referencedOn RoundTable.gameVariant
    var createdAt by RoundTable.createdAt
    var finishedAt by RoundTable.finishedAt
}
