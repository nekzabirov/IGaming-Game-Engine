package infrastructure.gamehub

/**
 * Credentials live in env now, not in a DB row — there is no `Aggregator` any more to hang them
 * off. [operatorId]/[operatorKey] are the SAME pair used both directions: casino-engine sends them
 * to the hub's `GatewayService`, and the hub sends them right back when it calls casino-engine's
 * own `WebhookService` — the check is symmetric on purpose.
 */
data class GameHubConfig(
    val grpcHost: String,

    val grpcPort: Int,

    val operatorId: String,

    val operatorKey: String,

    val plaintext: Boolean,
)
