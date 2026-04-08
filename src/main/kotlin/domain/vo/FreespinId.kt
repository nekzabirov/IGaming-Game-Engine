package domain.vo

import domain.exception.badrequest.BlankFreespinIdException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/** Identifier of a freespin promotion redeemed during the round. */
@Serializable
@JvmInline
value class FreespinId(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankFreespinIdException() }
    }
}
