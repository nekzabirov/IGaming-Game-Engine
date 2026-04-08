package application.query.aggregator

import application.IQuery
import domain.model.Aggregator
import domain.vo.Identity

data class BatchAggregatorQuery(
    val identities: List<Identity>,
) : IQuery<List<Aggregator>>
