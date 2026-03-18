package infrastructure.s3

import application.port.external.FileAdapter
import application.port.external.MediaFile
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import java.util.UUID

class S3FileAdapter(
    private val config: S3Config
) : FileAdapter {

    private val client = S3Client {
        region = config.region
        endpointUrl = Url.parse(config.endpoint)
        forcePathStyle = true
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.accessKey
            secretAccessKey = config.secretKey
        }
    }

    override suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String> = runCatching {
        val key = buildKey(folder, fileName, file.ext)

        client.putObject(PutObjectRequest {
            bucket = config.bucket
            this.key = key
            body = ByteStream.fromBytes(file.bytes)
        })

        key
    }

    override suspend fun delete(path: String): Result<Boolean> = runCatching {
        client.deleteObject(DeleteObjectRequest {
            bucket = config.bucket
            key = path
        })
        true
    }

    private fun buildKey(folder: String, fileName: String, ext: String): String {
        val name = fileName.ifBlank { UUID.randomUUID().toString() }
        return if (ext.isNotBlank()) "$folder/$name.$ext" else "$folder/$name"
    }
}
