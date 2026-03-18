package infrastructure.persistence.entity

import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpinEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SpinEntity>(SpinTable)

    var externalId by SpinTable.externalId
    var round by RoundEntity referencedOn SpinTable.round
    var reference by SpinEntity optionalReferencedOn SpinTable.reference
    var type by SpinTable.type
    var amount by SpinTable.amount
    var realAmount by SpinTable.realAmount
    var bonusAmount by SpinTable.bonusAmount
}
