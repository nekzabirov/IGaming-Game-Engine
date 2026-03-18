package infrastructure.handler.provider

import application.cqrs.ICommandHandler
import application.cqrs.provider.SetProviderImageCommand
import application.port.external.FileAdapter
import application.port.external.MediaFile
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SetProviderImageCommandHandler(
    private val fileAdapter: FileAdapter
) : ICommandHandler<SetProviderImageCommand, Unit> {
    override suspend fun handle(command: SetProviderImageCommand): Result<Unit> = runCatching {
        val ext = command.file.name.substringAfterLast('.', "")
        val mediaFile = MediaFile(ext = ext, bytes = command.file.content)

        val url = fileAdapter.upload("providers", command.identity.value, mediaFile).getOrThrow()

        newSuspendedTransaction {
            val entity = ProviderEntity.find { ProviderTable.identity eq command.identity.value }
                .firstOrNull() ?: throw IllegalArgumentException("Provider not found: ${command.identity.value}")

            entity.images = entity.images.toMutableMap().apply { put(command.key, url) }
        }
    }
}
