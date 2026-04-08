package infrastructure.handler.aggregator

import application.ICommandHandler
import application.command.aggregator.SaveAggregatorCommand
import domain.repository.IAggregatorRepository
import domain.model.Aggregator

class SaveAggregatorCommandHandler(
    private val aggregatorRepository: IAggregatorRepository,
) : ICommandHandler<SaveAggregatorCommand, Unit> {

    override suspend fun handle(command: SaveAggregatorCommand): Result<Unit> = runCatching {
        val existing = aggregatorRepository.findByIdentity(command.identity)
        val aggregator = existing?.copy(
            integration = command.integration,
            config = command.config,
            active = command.active,
        ) ?: Aggregator(
            identity = command.identity,
            integration = command.integration,
            config = command.config,
            active = command.active,
        )

        aggregatorRepository.save(aggregator)
    }
}
