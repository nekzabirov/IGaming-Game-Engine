package application.cqrs.aggregator

import application.cqrs.IQuery
import domain.model.Aggregator
import domain.vo.Identity
import java.util.Optional

data class FindAggregatorQuery(
    val identity: Identity,
) : IQuery<Optional<Aggregator>>
