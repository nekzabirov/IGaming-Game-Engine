package com.nekgamebling.application.port.inbound.game.query

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.provider.model.Provider
import kotlinx.datetime.LocalDateTime
import shared.value.Page
import shared.value.Pageable

data class FindAllGameWinsQuery(
    val pageable: Pageable,
    val gameIdentity: String? = null,
    val playerId: String? = null,
    val currency: String? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null
) : Query<FindAllGameWinsResponse>

data class FindAllGameWinsResponse(
    val items: Page<GameWonItem>,

    val providers: List<Provider>,
    val aggregators: List<AggregatorInfo>,
    val collections: List<Collection>
)

data class GameWonItem(
    val id: String,
    val game: GameItemView,
    val playerId: String,
    val amount: Long,
    val currency: String,
    val createdAt: LocalDateTime
)
