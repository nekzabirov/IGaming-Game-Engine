package domain.vo

import domain.exception.badrequest.BlankSessionTokenException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/**
 * Session token value object.
 */
@Serializable
@JvmInline
value class SessionToken(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankSessionTokenException() }
    }
}
