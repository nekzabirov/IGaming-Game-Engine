package domain.vo

import domain.exception.badrequest.BlankLocaleException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Locale(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankLocaleException() }
    }
}
