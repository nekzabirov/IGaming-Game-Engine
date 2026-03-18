package infrastructure.handler.aggregator

import application.cqrs.ICommandHandler
import application.cqrs.aggregator.SyncAllActiveAggregatorCommand
import application.port.storage.IAggregatorRepository
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