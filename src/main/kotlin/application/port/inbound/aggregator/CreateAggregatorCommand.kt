package com.nekgamebling.application.port.inbound.aggregator

import application.port.inbound.Command
import domain.aggregator.AggregatorInfo
import domain.common.value.Aggregator

data class CreateAggregatorCommand(
    val identity: String,
    val aggregator: Aggregator,
    val config: Map<String, String> = emptyMap(),
    val active: Boolean = true
) : Command<CreateAggregatorResponse>

data class CreateAggregatorResponse(val aggregator: AggregatorInfo)
