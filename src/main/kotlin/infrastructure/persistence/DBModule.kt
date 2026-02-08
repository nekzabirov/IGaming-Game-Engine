package infrastructure.persistence

import application.port.outbound.storage.AggregatorInfoRepository
import com.nekgamebling.application.port.outbound.storage.CacheAdapter
import application.port.outbound.GameSyncAdapter
import application.port.outbound.GameVariantRepository
import application.port.outbound.RoundRepository
import application.port.outbound.SpinRepository
import domain.game.repository.GameRepository
import domain.session.repository.SessionRepository
import infrastructure.persistence.cache.InMemoryCacheAdapter
import infrastructure.persistence.exposed.adapter.ExposedGameSyncAdapter
import infrastructure.persistence.exposed.repository.ExposedAggregatorInfoRepository
import infrastructure.persistence.exposed.repository.ExposedGameRepository
import infrastructure.persistence.exposed.repository.ExposedGameVariantRepository
import infrastructure.persistence.exposed.repository.ExposedRoundRepository
import infrastructure.persistence.exposed.repository.ExposedSessionRepository
import infrastructure.persistence.exposed.repository.ExposedSpinRepository
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Database configuration from environment variables.
 */
data class DatabaseEnvConfig(
    val jdbcUrl: String = System.getenv("DB_URL") ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    val username: String = System.getenv("DB_USER") ?: "",
    val password: String = System.getenv("DB_PASSWORD") ?: "",
    val poolProfile: String = System.getenv("DB_POOL_PROFILE") ?: "development"
)

val DBModule = module {
    databaseModule()
    repositoryModule()
    cacheModule()
}

/**
 * Configure database with HikariCP connection pool.
 */
private fun Module.databaseModule() {
    single<Database> {
        val envConfig = DatabaseEnvConfig()
        val poolConfig = when (envConfig.poolProfile.lowercase()) {
            "production" -> DatabasePoolConfig.production()
            "high-throughput" -> DatabasePoolConfig.highThroughput()
            else -> DatabasePoolConfig.development()
        }
        DatabaseConfig.configure(
            jdbcUrl = envConfig.jdbcUrl,
            username = envConfig.username,
            password = envConfig.password,
            config = poolConfig
        )
    }
}

private fun Module.repositoryModule() {
    // ==========================================
    // Essential Repositories
    // ==========================================
    single<GameRepository> { ExposedGameRepository() }
    single<SessionRepository> { ExposedSessionRepository() }
    single<RoundRepository> { ExposedRoundRepository() }
    single<SpinRepository> { ExposedSpinRepository() }
    single<AggregatorInfoRepository> { ExposedAggregatorInfoRepository() }
    single<GameVariantRepository> { ExposedGameVariantRepository() }

    // ==========================================
    // Adapters
    // ==========================================
    single<GameSyncAdapter> { ExposedGameSyncAdapter() }
}

private fun Module.cacheModule() {
    single<CacheAdapter> { InMemoryCacheAdapter() }
}
