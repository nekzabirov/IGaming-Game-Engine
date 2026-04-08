package domain.vo

import domain.exception.badrequest.BlankGameSymbolException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/**
 * Aggregator-side game symbol. Unique within an integration; used by aggregators to
 * identify which game variant the player is launching/spinning.
 */
@Serializable
@JvmInline
value class GameSymbol(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankGameSymbolException() }
    }
}
