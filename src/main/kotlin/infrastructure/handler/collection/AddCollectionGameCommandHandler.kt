package infrastructure.handler.collection

import application.ICommandHandler
import application.command.collection.AddCollectionGameCommand
import domain.repository.ICollectionRepository

class AddCollectionGameCommandHandler(
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<AddCollectionGameCommand, Unit> {

    override suspend fun handle(command: AddCollectionGameCommand): Result<Unit> = runCatching {
        collectionRepository.addGame(command.identity, command.gameIdentity)
    }
}
