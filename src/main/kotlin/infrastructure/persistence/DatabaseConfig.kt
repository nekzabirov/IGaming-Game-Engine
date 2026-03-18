package infrastructure.persistence

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2
)
