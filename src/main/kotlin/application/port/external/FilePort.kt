package application.port.external

/**
 * Media file data for upload.
 */
data class MediaFile(
    val ext: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaFile
        return ext == other.ext && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = ext.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * Port interface for file storage operations.
 */
interface FileAdapter {
    /**
     * Upload a file and return the path/URL.
     * @param folder The folder/prefix for the file
     * @param fileName The name of the file (without extension)
     * @param file The media file to upload
     * @return The path or URL of the uploaded file
     */
    suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String>

    /**
     * Delete a file by path.
     * @param path The path of the file to delete
     * @return true if deleted successfully
     */
    suspend fun delete(path: String): Result<Boolean>
}
