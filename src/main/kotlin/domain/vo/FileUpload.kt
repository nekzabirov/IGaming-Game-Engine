package domain.vo

import domain.exception.badrequest.BlankFileNameException
import domain.exception.badrequest.EmptyFileContentException
import domain.exception.domainRequire

data class FileUpload(
    val name: String,
    val content: ByteArray,
) {
    init {
        domainRequire(name.isNotBlank()) { BlankFileNameException() }
        domainRequire(content.isNotEmpty()) { EmptyFileContentException() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileUpload

        if (name != other.name) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
