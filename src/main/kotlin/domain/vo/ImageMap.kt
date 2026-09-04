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

    /** Right-biased union — [overrides] wins per key. Used to show the hub's synced artwork with
     *  a local override on top, without ever mutating either side. */
    fun mergedWith(overrides: ImageMap): ImageMap = ImageMap(data + overrides.data)

    companion object {
        val EMPTY: ImageMap
            get() = ImageMap(emptyMap())
    }
}
