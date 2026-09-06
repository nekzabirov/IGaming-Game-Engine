package plugins

import DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private lateinit var dbDispatcher: CoroutineDispatcher

/**
 * One Hikari pool, one Exposed database. JDBC blocks, so every transaction runs on a dispatcher
 * capped at the pool size: a coroutine never parks a thread waiting for a connection that N other
 * coroutines are already holding.
 */
fun connectDatabase(config: DbConfig) {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.user
        password = config.password
        maximumPoolSize = config.poolSize
        minimumIdle = minOf(2, config.poolSize)
        driverClassName = "org.postgresql.Driver"
        isAutoCommit = false
        poolName = "casino"
    })

    dbDispatcher = Dispatchers.IO.limitedParallelism(config.poolSize)

    Database.connect(
        dataSource,
        databaseConfig = DatabaseConfig {
            // Exposed retries a transaction on ANY SQLException up to this many times. A unique
            // violation on `spins.external_id` is an expected outcome (a redelivered leg), not a
            // transient fault, and three identical inserts before answering is pure waste.
            defaultMaxAttempts = 1
        },
    )
}

/**
 * A write transaction. The block must not call pam, GameHub, Redis or RabbitMQ: a remote round
 * trip inside a transaction holds a pooled connection for its whole duration, and a failure after
 * the remote side already acted leaves the two out of sync with nothing to reconcile them.
 */
suspend fun <T> dbTransaction(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(dbDispatcher, statement = block)

/** A read-only transaction, same rules as [dbTransaction]. */
suspend fun <T> dbRead(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(dbDispatcher, readOnly = true, statement = block)
