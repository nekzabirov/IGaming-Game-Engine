package domain.vo

import kotlinx.serialization.Serializable

/**
 * Image map containing image URLs keyed by type (e.g., "thumbnail", "banner").
 */
@Serializable
@JvmInline
value class ImageMap(val data: MutableMap<String, String>) {
    companion object {
        val EMPTY = ImageMap(mutableMapOf())
    }

    operator fun set(key: String, value: String) {
        data[key] = value
    }

    operator fun get(key: String) = data[key]

    fun remove(key: String) = data.remove(key)
}
