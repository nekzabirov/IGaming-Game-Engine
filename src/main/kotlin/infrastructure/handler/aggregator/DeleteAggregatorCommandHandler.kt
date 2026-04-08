package infrastructure.handler.aggregator

import application.ICommandHandler
import application.command.aggregator.DeleteAggregatorCommand
import domain.repository.IAggregatorRepository

class DeleteAggregatorCommandHandler(
    private val aggregatorRepository: IAggregatorRepository,
) : ICommandHandler<DeleteAggregatorCommand, Unit> {

    override suspend fun handle(command: DeleteAggregatorCommand): Result<Unit> = runCatching {
        aggregatorRepository.deleteByIdentity(command.identity)
    }
}
