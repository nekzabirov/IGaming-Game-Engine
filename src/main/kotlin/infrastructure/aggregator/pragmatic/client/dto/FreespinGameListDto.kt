package infrastructure.aggregator.pragmatic.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class FreespinGameListDto(
    val gameList: List<FreespinGameDto>
)

@Serializable
data class FreespinGameDto(
    val gameId: String,

    val betValues: List<FreespinBetValueDto>
)

@Serializable
data class FreespinBetValueDto(
    val currency: String,

    val totalBet: Double
)
