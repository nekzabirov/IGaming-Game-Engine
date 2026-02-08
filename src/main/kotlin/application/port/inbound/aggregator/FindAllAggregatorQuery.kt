package com.nekgamebling.application.port.inbound.aggregator

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import shared.value.Page
import shared.value.Pageable

data class FindAllAggregatorQuery(val pageable: Pageable, val query: String = "", val active: Boolean? = null) :
    Query<FindAllAggregatorResponse>

data class FindAllAggregatorResponse(val result: Page<AggregatorInfo>)
