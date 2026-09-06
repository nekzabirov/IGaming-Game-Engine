package support

import DbConfig
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import plugins.connectDatabase
import plugins.dbTransaction

/**
 * One Postgres for the whole test JVM, migrated by the real Flyway scripts — the search functions,
 * indexes and constraints the services rely on are the production ones, not a lookalike.
 */
object TestDatabase {

    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("casino")
            .withUsername("test")
            .withPassword("test")
            .also { it.start() }
    }

    private var connected = false

    @Synchronized
    fun connect() {
        if (connected) return
        val c = container
        Flyway.configure()
            .dataSource(c.jdbcUrl, c.username, c.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        connectDatabase(
            DbConfig(
                baseUrl = c.jdbcUrl.substringBeforeLast('/'),
                name = c.databaseName,
                user = c.username,
                password = c.password,
                poolSize = 4,
            ),
        )
        connected = true
    }

    suspend fun reset() {
        dbTransaction {
            exec(
                "TRUNCATE spins, casino_rounds, casino_game_favourites, casino_game_collections, " +
                    "collections, casino_games, casino_providers RESTART IDENTITY CASCADE",
            )
        }
    }
}
