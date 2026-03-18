package infrastructure.aggregator.pragmatic.webhook.dto

data class PragmaticBetDto(
    val reference: String,

    val gameId: String,

    val roundId: String,

    val bonusCode: String?,

    val amount: String,

    val promoWinAmount: String = "0"
)
