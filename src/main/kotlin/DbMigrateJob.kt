import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.sql.DriverManager

private val log = LoggerFactory.getLogger("DbMigrateJob")

/** Creates the database if it is missing, then applies Flyway migrations. Runs before every deploy. */
fun main() {
    val config = AppConfig.fromEnv().database

    createDatabaseIfMissing(config)

    log.info("Running Flyway migrations against database '{}'", config.name)
    val result = Flyway.configure()
        .dataSource(config.jdbcUrl, config.user, config.password)
        .locations("classpath:db/migration")
        .load()
        .migrate()
    log.info("Flyway finished: applied={} successful={}", result.migrationsExecuted, result.success)
}

private fun createDatabaseIfMissing(config: DbConfig) {
    DriverManager.getConnection("${config.baseUrl}/postgres", config.user, config.password).use { conn ->
        conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?").use { ps ->
            ps.setString(1, config.name)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    log.info("Database '{}' already exists", config.name)
                    return
                }
            }
        }
        conn.createStatement().use { st ->
            st.executeUpdate("CREATE DATABASE \"${config.name}\"")
            log.info("Database '{}' created", config.name)
        }
    }
}
