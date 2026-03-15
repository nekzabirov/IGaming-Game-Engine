package com.nekgamebling.application.port.inbound.game.query

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.provider.model.Provider
import shared.value.Page
import shared.value.Pageable

data class FindAllGameQuery(
    val pageable: Pageable,

    val query: String,

    val active: Boolean? = null,

    val providerIdentities: List<String>? = null,
    val collectionIdentities: List<String>? = null,

    val tags: List<String>? = null,

    val bonusBetEnable: Boolean? = null,
    val bonusWageringEnable: Boolean? = null,

    val freeSpinEnable: Boolean? = null,
    val freeChipEnable: Boolean? = null,
    val jackpotEnable: Boolean? = null,
) : Query<FindAllGameResponse>

data class FindAllGameResponse(
    val result: Page<GameItemView>,

    val providers: List<Provider>,

    val aggregators: List<AggregatorInfo>,

    val collections: List<Collection>
)
