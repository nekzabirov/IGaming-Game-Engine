package infrastructure.persistence.table

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json

private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())
private val stringListSerializer = ListSerializer(String.serializer())

object GameTable : LongIdTable("games") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val provider = reference("provider_id", ProviderTable)
    val bonusBetEnable = bool("bonus_bet_enable").default(true)
    val bonusWageringEnable = bool("bonus_wagering_enable").default(true)
    val tags = json(
        "tags",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    )
    val active = bool("active")
    val images = json(
        "images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    )
    val sortOrder = integer("sort_order")
}
