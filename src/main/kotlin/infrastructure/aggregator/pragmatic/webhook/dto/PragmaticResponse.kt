package infrastructure.aggregator.pragmatic.webhook.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class PragmaticResponse {

    @Serializable
    data class Success(
        val cash: String,

        val bonus: String,

        val currency: String,

        val userId: String? = null,

        val transactionId: String? = null,

        val usedPromo: String? = null
    ) : PragmaticResponse()

    @Serializable
    data class Error(
        val error: Int,

        val description: String
    ) : PragmaticResponse() {
        companion object {
            val UNEXPECTED_ERROR = Error(
                error = 1,
                description = "Internal Server Error"
            )

            val SESSION_EXPIRED = Error(
                error = 2,
                description = "Player not found or session expired"
            )

            val INSUFFICIENT_FUNDS = Error(
                error = 3,
                description = "Not enough balance"
            )

            val BET_LIMIT_EXCEEDED = Error(
                error = 4,
                description = "Bet limit exceeded"
            )
        }
    }
}
