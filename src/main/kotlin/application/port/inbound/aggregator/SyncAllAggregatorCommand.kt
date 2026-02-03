package com.nekgamebling.application.port.inbound.aggregator

import application.port.inbound.Command

data object SyncAllAggregatorCommand : Command<SyncAllAggregatorResponse>

data class SyncAllAggregatorResponse(val totalGames: Int)