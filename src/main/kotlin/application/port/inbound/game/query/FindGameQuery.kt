package com.nekgamebling.application.port.inbound.game.query

import application.port.inbound.Query
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.provider.model.Provider

data class FindGameQuery(val identity: String) : Query<FindGameResponse>

data class FindGameResponse(
    val game: Game,

    val provider: Provider,

    val activeVariant: GameVariant,

    val aggregator: AggregatorInfo,

    val collections: List<Collection>
)
