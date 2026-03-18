package infrastructure.handler.collection

import application.cqrs.ICommandHandler
import application.cqrs.collection.SetCollectionImageCommand
import application.port.external.FileAdapter
import application.port.external.MediaFile
import infrastructure.persistence.entity.CollectionEntity
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SetCollectionImageCommandHandler(
    private val fileAdapter: FileAdapter
) : ICommandHandler<SetCollectionImageCommand, Unit> {
    override suspend fun handle(command: SetCollectionImageCommand): Result<Unit> = runCatching {
        val ext = command.file.name.substringAfterLast('.', "")
        val mediaFile = MediaFile(ext = ext, bytes = command.file.content)

        val url = fileAdapter.upload("collections", command.identity.value, mediaFile).getOrThrow()

        newSuspendedTransaction {
            val entity = CollectionEntity.find { CollectionTable.identity eq command.identity.value }
                .firstOrNull() ?: throw IllegalArgumentException("Collection not found: ${command.identity.value}")

            entity.images = entity.images.toMutableMap().apply { put(command.key, url) }
        }
    }
}
