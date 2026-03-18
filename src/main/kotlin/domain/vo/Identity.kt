package domain.vo

import domain.exception.badrequest.EmptyIdentityException
import domain.exception.badrequest.InvalidIdentityFormatException
import domain.exception.badrequest.InvalidIdentityGenerationException
import domain.exception.domainRequire

@JvmInline
value class Identity(val value: String) {
    init {
        domainRequire(value.isNotEmpty()) { EmptyIdentityException() }
        domainRequire(value.matches(Regex("^[a-z0-9_]+$"))) { InvalidIdentityFormatException() }
    }

    companion object {
        fun generate(input: String): Identity {
            val converted = input
                .trim()
                .lowercase()
                .replace(Regex("\\s+"), "_")
                .replace(Regex("[^a-z0-9_]"), "")
                .replace(Regex("_+"), "_")
                .trimEnd('_')
                .trimStart('_')
            domainRequire(converted.isNotEmpty()) { InvalidIdentityGenerationException(input) }
            return Identity(converted)
        }
    }

    override fun toString(): String = value
}