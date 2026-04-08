package domain.vo

import domain.exception.badrequest.BlankExternalIdException
import domain.exception.domainRequire
import kotlinx.serialization.Serializable

/** External spin identifier issued by the aggregator. */
@Serializable
@JvmInline
value class ExternalSpinId(val value: String) {
    init {
        domainRequire(value.isNotBlank()) { BlankExternalIdException() }
    }
}
