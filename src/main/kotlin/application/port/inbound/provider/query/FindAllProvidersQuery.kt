package com.nekgamebling.application.port.inbound.provider.query

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import domain.provider.model.Provider
import shared.value.Page
import shared.value.Pageable

data class FindAllProvidersQuery(
    val pageable: Pageable,
    val query: String = "",
    val active: Boolean? = null,
    val aggregatorIdentity: String? = null
) : Query<FindAllProvidersResponse>

data class FindAllProvidersResponse(
    val result: Page<ProviderItem>,
    val aggregators: List<AggregatorInfo>
) {
    data class ProviderItem(
        val provider: Provider,
        val aggregatorIdentity: String,
        val activeGames: Int,
        val totalGames: Int
    )
}
