package infrastructure.persistence.exposed.mapper

import domain.aggregator.AggregatorInfo
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAggregatorInfo(): AggregatorInfo = AggregatorInfo(
    id = this[AggregatorInfoTable.id].value,
    identity = this[AggregatorInfoTable.identity],
    config = this[AggregatorInfoTable.config],
    aggregator = this[AggregatorInfoTable.aggregator],
    active = this[AggregatorInfoTable.active]
)
