package infrastructure.persistence.table

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json

private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

object ProviderTable : LongIdTable("providers") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val images = json<Map<String, String>>(
        "images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    )
    val sortOrder = integer("sort_order").default(100)
    val active = bool("active").default(false)
    val aggregator = reference("aggregator_id", AggregatorTable)
}
