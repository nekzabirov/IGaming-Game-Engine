package infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import infrastructure.persistence.table.RoundTable
import infrastructure.persistence.table.SessionTable
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {

    fun init(config: DatabaseConfig) {
        val dataSource = createHikariDataSource(config)
        Database.connect(dataSource)
    }

    suspend fun createTables() {
        newSuspendedTransaction {
            SchemaUtils.create(
                AggregatorTable,
                ProviderTable,
                CollectionTable,
                GameTable,
                GameVariantTable,
                GameCollectionTable,
                GameFavouriteTable,
                SessionTable,
                RoundTable,
                SpinTable
            )
        }
    }

    private fun createHikariDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle
            driverClassName = "org.postgresql.Driver"
            isAutoCommit = false
        }
        return HikariDataSource(hikariConfig)
    }
}
