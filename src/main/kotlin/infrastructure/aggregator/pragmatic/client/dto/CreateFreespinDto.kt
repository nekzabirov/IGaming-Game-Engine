package infrastructure.aggregator.pragmatic.client.dto

data class CreateFreespinDto(
    val bonusCode: String,

    val playerId: String,

    val currency: String,

    val rounds: Int,

    val startTimestamp: Long,

    val expirationTimestamp: Long,

    val gameList: List<FreespinGameDto>
)
