package infrastructure.handler.aggregator

import application.ICommandHandler
import application.command.aggregator.SyncAllActiveAggregatorCommand
import domain.repository.IAggregatorRepository
import application.usecase.SyncAggregatorUsecase

class SyncAllActiveAggregatorCommandHandler(
    private val aggregatorRepository: IAggregatorRepository,
    private val syncAggregatorUsecase: SyncAggregatorUsecase
) : ICommandHandler<SyncAllActiveAggregatorCommand, Unit> {

    override suspend fun handle(command: SyncAllActiveAggregatorCommand): Result<Unit> = runCatching {
        val aggregators = aggregatorRepository.findAll().filter { it.active }

        for (aggregator in aggregators) {
            syncAggregatorUsecase(aggregator)
        }
    }

}