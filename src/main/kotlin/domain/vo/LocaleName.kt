package domain.vo

import kotlinx.serialization.Serializable

/**
 * Localized name map containing names keyed by locale.
 */
@Serializable
@JvmInline
value class LocaleName(val data: Map<String, String>) {
    fun get(locale: Locale): String? = data[locale.value]
    fun get(locale: String): String? = data[locale]

    companion object {
        val EMPTY = LocaleName(emptyMap())
    }
}
