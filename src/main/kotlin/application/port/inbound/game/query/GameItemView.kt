package com.nekgamebling.application.port.inbound.game.query

import domain.game.model.Game
import domain.game.model.GameVariant

data class GameItemView(
    val game: Game,
    val activeVariant: GameVariant,
    val collectionIdentities: List<String>
)
