package domain.vo

import domain.exception.badrequest.InvalidAmountException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/** Monetary amount in minor units (cents). */
@Serializable
@JvmInline
value class Amount(val value: Long) {
    init {
        domainRequire(value >= 0) { InvalidAmountException(value) }
    }

    operator fun plus(other: Amount): Amount = Amount(value + other.value)

    operator fun minus(other: Amount): Amount = Amount(value - other.value)

    operator fun compareTo(other: Amount): Int = value.compareTo(other.value)

    companion object {
        val ZERO: Amount
            get() = Amount(0)
    }
}

fun minOf(a: Amount, b: Amount): Amount = Amount(minOf(a.value, b.value))
