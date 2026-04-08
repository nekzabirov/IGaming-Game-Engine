package infrastructure.handler.aggregator

import application.IQueryHandler
import application.query.aggregator.BatchAggregatorQuery
import domain.model.Aggregator
import domain.repository.IAggregatorRepository

class BatchAggregatorQueryHandler(
    private val aggregatorRepository: IAggregatorRepository,
) : IQueryHandler<BatchAggregatorQuery, List<Aggregator>> {

    override suspend fun handle(query: BatchAggregatorQuery): List<Aggregator> =
        aggregatorRepository.findAllByIdentities(query.identities)
}
