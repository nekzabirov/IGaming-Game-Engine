package infrastructure.persistence.entity

import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class AggregatorEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AggregatorEntity>(AggregatorTable)

    var identity by AggregatorTable.identity
    var integration by AggregatorTable.integration
    var config by AggregatorTable.config
    var active by AggregatorTable.active
}
