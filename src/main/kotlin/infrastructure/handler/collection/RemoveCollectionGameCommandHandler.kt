package infrastructure.handler.collection

import application.ICommandHandler
import application.command.collection.RemoveCollectionGameCommand
import domain.repository.ICollectionRepository

class RemoveCollectionGameCommandHandler(
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<RemoveCollectionGameCommand, Unit> {

    override suspend fun handle(command: RemoveCollectionGameCommand): Result<Unit> = runCatching {
        collectionRepository.removeGame(command.identity, command.gameIdentity)
    }
}
