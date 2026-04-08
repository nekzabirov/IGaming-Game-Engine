package application.query.aggregator

import application.IQuery
import domain.model.Aggregator
import domain.vo.Page
import domain.vo.Pageable

data class FindAllAggregatorQuery(
    val query: String,

    val integration: String? = null,

    val active: Boolean? = null,

    val pageable: Pageable,
) : IQuery<Page<Aggregator>>
