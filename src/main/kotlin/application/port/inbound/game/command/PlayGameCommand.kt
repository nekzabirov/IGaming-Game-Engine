package com.nekgamebling.application.port.inbound.game.command

import application.port.inbound.Command
import domain.common.value.Locale
import domain.common.value.Platform
import shared.value.Currency

data class PlayGameCommand(
    val identity: String,
    val playerId: String,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform,
    val lobbyUrl: String,
    val spinLimitAmount: Long? = null
) : Command<PlayGameResponse>

data class PlayGameResponse(
    val launchUrl: String
)
