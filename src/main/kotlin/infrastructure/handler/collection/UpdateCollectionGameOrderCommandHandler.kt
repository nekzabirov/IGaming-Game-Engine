package infrastructure.handler.collection

import application.ICommandHandler
import application.command.collection.UpdateCollectionGameOrderCommand
import domain.repository.ICollectionRepository

class UpdateCollectionGameOrderCommandHandler(
    private val collectionRepository: ICollectionRepository,
) : ICommandHandler<UpdateCollectionGameOrderCommand, Unit> {

    override suspend fun handle(command: UpdateCollectionGameOrderCommand): Result<Unit> = runCatching {
        collectionRepository.updateGameOrder(
            identity = command.identity,
            gameIdentity = command.gameIdentity,
            order = command.order,
        )
    }
}
