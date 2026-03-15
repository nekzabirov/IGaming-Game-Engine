package com.nekgamebling.application.port.inbound.spin

import application.port.inbound.Query
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.provider.model.Provider
import domain.session.model.Round
import shared.value.Currency

data class FindRoundQuery(
    val id: String
) : Query<FindRoundQueryResult>

data class FindRoundQueryResult(
    val round: Round,
    val game: GameItemView,
    val playerId: String,
    val currency: Currency,
    val totalPlaceReal: Long,
    val totalPlaceBonus: Long,
    val totalSettleReal: Long,
    val totalSettleBonus: Long,

    val providers: List<Provider>,
    val aggregators: List<AggregatorInfo>,
    val collections: List<Collection>
)
