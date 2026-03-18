package infrastructure.persistence.mapper

import domain.model.Aggregator
import domain.vo.Identity
import infrastructure.persistence.entity.AggregatorEntity
import infrastructure.persistence.table.AggregatorTable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.jetbrains.exposed.sql.ResultRow

object AggregatorMapper {

    fun AggregatorEntity.toDomain(): Aggregator = Aggregator(
        identity = Identity(identity),
        integration = integration,
        config = config.toMap(),
        active = active,
    )

    fun ResultRow.toAggregator(): Aggregator = Aggregator(
        identity = Identity(this[AggregatorTable.identity]),
        integration = this[AggregatorTable.integration],
        config = this[AggregatorTable.config].toMap(),
        active = this[AggregatorTable.active],
    )

    private fun JsonObject.toMap(): Map<String, Any> = entries.associate { (key, element) ->
        key to element.toAny()
    }

    private fun kotlinx.serialization.json.JsonElement.toAny(): Any = when (this) {
        is JsonPrimitive -> when {
            booleanOrNull != null -> booleanOrNull!!
            longOrNull != null -> longOrNull!!
            doubleOrNull != null -> doubleOrNull!!
            else -> contentOrNull ?: ""
        }
        is JsonObject -> toMap()
        is JsonArray -> map { it.toAny() }
        is JsonNull -> ""
    }
}
