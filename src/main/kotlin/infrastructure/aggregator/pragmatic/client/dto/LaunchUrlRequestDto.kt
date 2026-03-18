package infrastructure.aggregator.pragmatic.client.dto

import domain.model.Platform

data class LaunchUrlRequestDto(
    val gameSymbol: String,

    val sessionToken: String,

    val playerId: String,

    val locale: String,

    val platform: Platform,

    val currency: String,

    val lobbyUrl: String,

    val demo: Boolean
)
