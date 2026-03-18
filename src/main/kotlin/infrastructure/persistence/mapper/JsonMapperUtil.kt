package infrastructure.persistence.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object JsonMapperUtil {
    fun Map<String, Any>.toJsonObject(): JsonObject =
        JsonObject(mapValues { (_, value) -> value.toJsonElement() })

    @Suppress("UNCHECKED_CAST")
    fun Any.toJsonElement(): JsonElement = when (this) {
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> (this as Map<String, Any>).toJsonObject()
        is List<*> -> JsonArray(map { (it ?: JsonNull).toJsonElement() })
        else -> JsonPrimitive(toString())
    }
}
