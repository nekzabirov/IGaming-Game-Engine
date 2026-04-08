package infrastructure.handler.collection

import application.ICommandHandler
import application.command.collection.DeleteCollectionCommand
import domain.repository.ICollectionRepository

class DeleteCollectionCommandHandler(
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<DeleteCollectionCommand, Unit> {

    override suspend fun handle(command: DeleteCollectionCommand): Result<Unit> = runCatching {
        collectionRepository.deleteByIdentity(command.identity)
    }
}
