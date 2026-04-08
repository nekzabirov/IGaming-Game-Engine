package infrastructure.handler.collection

import application.ICommandHandler
import application.command.collection.SaveCollectionCommand
import domain.repository.ICollectionRepository
import domain.model.Collection

class SaveCollectionCommandHandler(
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<SaveCollectionCommand, Unit> {

    override suspend fun handle(command: SaveCollectionCommand): Result<Unit> = runCatching {
        val existing = collectionRepository.findByIdentity(command.identity)
        val collection = existing?.copy(
            name = command.name,
            active = command.active,
            order = command.order,
        ) ?: Collection(
            identity = command.identity,
            name = command.name,
            active = command.active,
            order = command.order,
        )

        collectionRepository.save(collection)
    }
}
