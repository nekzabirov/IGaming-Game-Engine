package com.nekgamebling.application.port.inbound.aggregator

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo

data class FindAggregatorQuery(val identity: String) : Query<FindAggregatorResponse>

data class FindAggregatorResponse(val aggregator: AggregatorInfo)
