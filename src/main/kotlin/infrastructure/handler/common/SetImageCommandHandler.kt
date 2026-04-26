package infrastructure.handler.common

import application.ICommandHandler
import application.command.collection.SetCollectionImageCommand
import application.command.common.SetImageCommand
import application.command.game.SetGameImageCommand
import application.command.provider.SetProviderImageCommand
import application.port.external.FileAdapter
import application.port.external.MediaFile
import domain.repository.ICollectionRepository
import domain.repository.IGameRepository
import domain.repository.IProviderRepository

/**
 * Single entry point for every `SetXImageCommand`.
 *
 * Replaces the 3 previously identical per-entity handlers. Uploads the media file
 * via [FileAdapter] then dispatches to the correct repository's `addImage(...)`
 * based on the concrete command subtype.
 */
class SetImageCommandHandler(
    private val fileAdapter: FileAdapter,
    private val gameRepository: IGameRepository,
    private val providerRepository: IProviderRepository,
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<SetImageCommand, Unit> {

    override suspend fun handle(command: SetImageCommand): Result<Unit> = runCatching {
        val ext = command.file.name.substringAfterLast('.', "")
        val media = MediaFile(ext = ext, bytes = command.file.content)
        val url = fileAdapter
            .upload(
                folder = command.folder,
                fileName = "${command.identity.value}/${command.key}",
                file = media,
            )
            .getOrThrow()

        when (command) {
            is SetGameImageCommand -> gameRepository.addImage(command.identity, command.key, url)
            is SetProviderImageCommand -> providerRepository.addImage(command.identity, command.key, url)
            is SetCollectionImageCommand -> collectionRepository.addImage(command.identity, command.key, url)
            else -> error("Unhandled SetImageCommand subtype: ${command::class.qualifiedName}")
        }
    }
}
