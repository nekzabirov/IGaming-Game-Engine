package domain.vo

import domain.exception.badrequest.BlankPlayerIdException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PlayerId(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankPlayerIdException() }
    }
}
