package domain.vo

import kotlinx.serialization.Serializable

/**
 * Immutable map of image URLs keyed by type (e.g., "thumbnail", "banner").
 *
 * Mutator methods (`with`, `without`) return a new instance — the underlying map is
 * read-only so external code cannot mutate the value object's state through a leaked
 * reference.
 */
@Serializable
@JvmInline
value class ImageMap(val data: Map<String, String>) {

    operator fun get(key: String): String? = data[key]

    fun with(key: String, value: String): ImageMap = ImageMap(data + (key to value))

    fun without(key: String): ImageMap = ImageMap(data - key)

    companion object {
        val EMPTY: ImageMap
            get() = ImageMap(emptyMap())
    }
}
