package infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init(config: DatabaseConfig) {
        val dataSource = createHikariDataSource(config)
        Database.connect(dataSource)
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
