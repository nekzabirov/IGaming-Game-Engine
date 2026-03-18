package domain.vo

import domain.exception.badrequest.BlankCurrencyException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/**
 * Currency value object representing a currency code (e.g., "USD", "EUR").
 */
@Serializable
@JvmInline
value class Currency(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankCurrencyException() }
    }
}
