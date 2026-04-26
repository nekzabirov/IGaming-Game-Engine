import infrastructure.persistence.CASINO_DB_NAME
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.sql.DriverManager

private val logger = LoggerFactory.getLogger("com.nekgamebling.DbMigrateJob")

fun main() {
    val baseUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432"
    val user = System.getenv("DB_USERNAME") ?: "user"
    val password = System.getenv("DB_PASSWORD") ?: "password"

    createDatabaseIfMissing(baseUrl, user, password)

    logger.info("Running Flyway migrations against database '{}'", CASINO_DB_NAME)
    val result = Flyway.configure()
        .dataSource("$baseUrl/$CASINO_DB_NAME", user, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()
    logger.info("Flyway finished: applied={} successful={}", result.migrationsExecuted, result.success)
}

private fun createDatabaseIfMissing(baseUrl: String, user: String, password: String) {
    DriverManager.getConnection("$baseUrl/postgres", user, password).use { conn ->
        conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?").use { ps ->
            ps.setString(1, CASINO_DB_NAME)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    logger.info("Database '{}' already exists", CASINO_DB_NAME)
                    return
                }
            }
        }
        conn.createStatement().use { st ->
            st.executeUpdate("CREATE DATABASE \"$CASINO_DB_NAME\"")
            logger.info("Database '{}' created", CASINO_DB_NAME)
        }
    }
}
