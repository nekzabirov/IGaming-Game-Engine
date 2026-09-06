/** Every environment variable the engine reads, resolved once at boot. */
data class AppConfig(
    val httpPort: Int,
    val grpcPort: Int,
    val database: DbConfig,
    val pam: PamConfig,
    val redis: RedisConfig,
    val rabbit: RabbitConfig,
    val gameHub: GameHubConfig,
    val eventExchange: String,
    /**
     * Who pays a free round's winnings. On (default): credited to the real balance here like any
     * win. Off: no spin of a free round touches the wallet — the win leaves only as a SpinEvent and
     * the promotion's owner (crm) settles it, so a bonus is paid once, by one owner.
     */
    val freespinToPayout: Boolean,
) {
    companion object {
        fun fromEnv(env: (String) -> String? = System::getenv): AppConfig = AppConfig(
            httpPort = env("HTTP_PORT")?.toIntOrNull() ?: 8080,
            grpcPort = env("GRPC_PORT")?.toIntOrNull() ?: 5050,
            database = DbConfig(
                baseUrl = env("DB_URL") ?: "jdbc:postgresql://localhost:5432",
                name = env("DATABASE_NAME") ?: "casino",
                user = env("DB_USERNAME") ?: "user",
                password = env("DB_PASSWORD") ?: "password",
                poolSize = env("DB_POOL_SIZE")?.toIntOrNull() ?: 10,
            ),
            pam = PamConfig(
                host = env("PAM_GRPC_HOST") ?: "localhost",
                port = env("PAM_GRPC_PORT")?.toIntOrNull() ?: 9090,
            ),
            redis = RedisConfig(
                host = env("REDIS_HOST") ?: "localhost",
                port = env("REDIS_PORT")?.toIntOrNull() ?: 6379,
            ),
            rabbit = RabbitConfig(
                host = env("RABBIT_HOST") ?: "localhost",
                port = env("RABBIT_PORT")?.toIntOrNull() ?: 5672,
                user = env("RABBIT_USER") ?: "guest",
                password = env("RABBIT_PASSWORD") ?: "guest",
                tls = env("RABBIT_TLS")?.toBoolean() ?: false,
            ),
            gameHub = GameHubConfig(
                host = env("GAMEHUB_GRPC_HOST") ?: "localhost",
                port = env("GAMEHUB_GRPC_PORT")?.toIntOrNull() ?: 443,
                operatorId = env("GAMEHUB_OPERATOR_ID") ?: "",
                operatorKey = env("GAMEHUB_OPERATOR_KEY") ?: "",
                plaintext = env("GAMEHUB_GRPC_PLAINTEXT")?.toBoolean() ?: false,
            ),
            eventExchange = env("EVENT_EXCHANGE") ?: "crm.exchange",
            freespinToPayout = env("FREESPIN_TO_PAYOUT")?.toBooleanStrictOrNull() ?: true,
        )
    }
}

data class DbConfig(
    val baseUrl: String,
    val name: String,
    val user: String,
    val password: String,
    val poolSize: Int = 10,
) {
    val jdbcUrl: String get() = "$baseUrl/$name"
}

data class PamConfig(val host: String, val port: Int)

data class RedisConfig(val host: String, val port: Int)

data class RabbitConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val tls: Boolean,
)

/**
 * [operatorId]/[operatorKey] are the SAME pair in both directions: sent to the hub's GatewayService,
 * and checked when the hub calls our WebhookService back.
 */
data class GameHubConfig(
    val host: String,
    val port: Int,
    val operatorId: String,
    val operatorKey: String,
    val plaintext: Boolean,
)
