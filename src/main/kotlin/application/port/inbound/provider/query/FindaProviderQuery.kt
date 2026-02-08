package com.nekgamebling.application.port.inbound.provider.query

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import domain.provider.model.Provider

data class FindaProviderQuery(val identity: String) : Query<FindaProviderResponse>

data class FindaProviderResponse(
    val provider: Provider,

    val aggregator: AggregatorInfo,

    val activeGames: Int,

    val totalGames: Int
)
