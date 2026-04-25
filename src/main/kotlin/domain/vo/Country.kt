package domain.vo

import domain.exception.badrequest.BlankCountryException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Country(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankCountryException() }
    }
}
