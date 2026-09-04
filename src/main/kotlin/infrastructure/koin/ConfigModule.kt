package infrastructure.koin

import infrastructure.gamehub.GameHubConfig
import infrastructure.pam.PamConfig
import infrastructure.persistence.CASINO_DB_NAME
import infrastructure.persistence.DatabaseConfig
import infrastructure.rabbitmq.RabbitMqConfig
import infrastructure.redis.RedisConfig
import org.koin.dsl.module

val configModule = module {
    single {
        val baseUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432"
        DatabaseConfig(
            url = "$baseUrl/$CASINO_DB_NAME",
            user = System.getenv("DB_USERNAME") ?: "user",
            password = System.getenv("DB_PASSWORD") ?: "password"
        )
    }
    single {
        PamConfig(
            address = System.getenv("PAM_GRPC_HOST") ?: "localhost",
            port = (System.getenv("PAM_GRPC_PORT") ?: "9090").toInt()
        )
    }
    single {
        RedisConfig(
            host = System.getenv("REDIS_HOST") ?: "localhost",
            port = (System.getenv("REDIS_PORT") ?: "6379").toInt()
        )
    }
    single {
        RabbitMqConfig(
            host = System.getenv("RABBIT_HOST") ?: "localhost",
            port = (System.getenv("RABBIT_PORT") ?: "5672").toInt(),
            user = System.getenv("RABBIT_USER") ?: "guest",
            password = System.getenv("RABBIT_PASSWORD") ?: "guest",
            tls = System.getenv("RABBIT_TLS")?.toBoolean() ?: false,
        )
    }
    single {
        GameHubConfig(
            grpcHost = System.getenv("GAMEHUB_GRPC_HOST") ?: "localhost",
            grpcPort = (System.getenv("GAMEHUB_GRPC_PORT") ?: "443").toInt(),
            operatorId = System.getenv("GAMEHUB_OPERATOR_ID") ?: "",
            operatorKey = System.getenv("GAMEHUB_OPERATOR_KEY") ?: "",
            plaintext = System.getenv("GAMEHUB_GRPC_PLAINTEXT")?.toBoolean() ?: false,
        )
    }
}
