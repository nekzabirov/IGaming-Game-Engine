package infrastructure.handler.game

import application.cqrs.ICommandHandler
import application.cqrs.game.SetGameImageCommand
import application.port.external.FileAdapter
import application.port.external.MediaFile
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SetGameImageCommandHandler(
    private val fileAdapter: FileAdapter
) : ICommandHandler<SetGameImageCommand, Unit> {
    override suspend fun handle(command: SetGameImageCommand): Result<Unit> = runCatching {
        val ext = command.file.name.substringAfterLast('.', "")
        val mediaFile = MediaFile(ext = ext, bytes = command.file.content)

        val url = fileAdapter.upload("games", command.identity.value, mediaFile).getOrThrow()

        newSuspendedTransaction {
            val entity = GameEntity.find { GameTable.identity eq command.identity.value }
                .firstOrNull() ?: throw IllegalArgumentException("Game not found: ${command.identity.value}")

            entity.images = entity.images.toMutableMap().apply { put(command.key, url) }
        }
    }
}
