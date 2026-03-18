package infrastructure.aggregator.onegamehub.webhook.dto

data class OneGameHubBetDto(
    val gameSymbol: String,

    val roundId: String,

    val transactionId: String,

    val freeSpinId: String?,

    val amount: Long,

    val finishRound: Boolean = false
)
