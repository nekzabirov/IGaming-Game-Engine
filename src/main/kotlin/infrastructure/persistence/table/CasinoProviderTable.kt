package infrastructure.persistence.table

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json

private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())
private val stringListSerializer = ListSerializer(String.serializer())

object CasinoProviderTable : LongIdTable("casino_providers") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val images = json<Map<String, String>>(
        "images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    )

    // Local-only, never written by sync. Wins per key over `images` on the wire.
    val customImages = json<Map<String, String>>(
        "custom_images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    ).default(emptyMap())

    val sortOrder = integer("sort_order").default(100)
    val active = bool("active").default(false)
    val blockedCountry = json<List<String>>(
        "blocked_country",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    ).default(emptyList())
    val tags = json<List<String>>(
        "tags",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    ).default(emptyList())

    // Local-only, never written by sync — same deal as `custom_images`, but for `tags`.
    val customTags = json<List<String>>(
        "custom_tags",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    ).default(emptyList())
}
