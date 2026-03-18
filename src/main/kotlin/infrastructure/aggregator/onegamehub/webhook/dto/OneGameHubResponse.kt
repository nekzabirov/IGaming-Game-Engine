package infrastructure.aggregator.onegamehub.webhook.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class OneGameHubResponse(val status: Int) {
    @Serializable
    data class Success(
        val balance: Int,
        val currency: String,
    ) : OneGameHubResponse(200)

    @Serializable
    data class Error(
        val code: String,
        val display: Boolean,
        val action: String,
        val message: String,
        val description: String,
    ) : OneGameHubResponse(400) {
        companion object {
            val UNEXPECTED_ERROR = Error(
                code = "ERR001",
                display = false,
                action = "restart",
                message = "Something wrong with request.",
                description = "Invalid request. Please try again."
            )

            val SESSION_TIMEOUT = Error(
                code = "ERR002",
                display = true,
                action = "restart",
                message = "Session expired, please login again.",
                description = "Session expired or not exits"
            )

            val INSUFFICIENT_FUNDS = Error(
                code = "ERR003",
                display = true,
                action = "restart",
                message = "Insufficient funds.",
                description = "Insufficient funds"
            )

            val EXCEED_WAGER_LIMIT = Error(
                code = "ERR004",
                display = true,
                action = "restart",
                message = "Exceed wager limit.",
                description = "Exceed wager limit"
            )

            val AUTH_FAILED = Error(
                code = "ERR005",
                display = true,
                action = "restart",
                message = "Authentication failed.",
                description = "Authentication failed"
            )

            val UNAUTHORIZED = Error(
                code = "ERR006",
                display = true,
                action = "restart",
                message = "Unauthorized.",
                description = "Unauthorized"
            )

            val UNSUPPORTED_CURRENCY = Error(
                code = "ERR007",
                display = true,
                action = "restart",
                message = "Unsupported currency.",
                description = "Unsupported currency"
            )

            val BONUS_BET_MAX_RESTRICTION = Error(
                code = "ERR008",
                display = true,
                action = "restart",
                message = "Bonus bet max restriction.",
                description = "Bonus bet max restriction"
            )
        }
    }
}
