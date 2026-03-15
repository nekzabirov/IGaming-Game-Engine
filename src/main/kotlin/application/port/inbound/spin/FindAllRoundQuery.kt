package com.nekgamebling.application.port.inbound.spin

import application.port.inbound.Query
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.provider.model.Provider
import domain.session.model.Round
import shared.value.Currency
import kotlinx.datetime.LocalDateTime
import shared.value.Page
import shared.value.Pageable

data class FindAllRoundQuery(
    val pageable: Pageable,

    val gameIdentity: String?,
    val providerIdentity: String?,
    val finished: Boolean?,
    val playerId: String?,
    val freeSpinId: String?,

    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,

    // Amount range filters
    val minPlaceAmount: Long? = null,
    val maxPlaceAmount: Long? = null,
    val minSettleAmount: Long? = null,
    val maxSettleAmount: Long? = null
) : Query<FindAllRoundQueryResult>

data class FindAllRoundQueryResult(
    val items: Page<RoundItem>,
    val providers: List<Provider>,
    val aggregators: List<AggregatorInfo>,
    val collections: List<Collection>
)

data class RoundItem(
    val round: Round,

    val game: GameItemView,

    val playerId: String,
    val currency: Currency,

    val totalPlaceReal: Long,
    val totalPlaceBonus: Long,
    val totalSettleReal: Long,
    val totalSettleBonus: Long
)
