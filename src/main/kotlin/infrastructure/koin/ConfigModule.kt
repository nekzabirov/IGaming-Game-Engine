package infrastructure.koin

import infrastructure.persistence.CASINO_DB_NAME
import infrastructure.persistence.DatabaseConfig
import infrastructure.rabbitmq.RabbitMqConfig
import infrastructure.redis.RedisConfig
import infrastructure.s3.S3Config
import infrastructure.wallet.WalletConfig
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
        WalletConfig(
            address = System.getenv("WALLET_GRPC_HOST") ?: "localhost",
            port = (System.getenv("WALLET_GRPC_PORT") ?: "5555").toInt()
        )
    }
    single {
        RedisConfig(
            host = System.getenv("REDIS_HOST") ?: "localhost",
            port = (System.getenv("REDIS_PORT") ?: "6379").toInt()
        )
    }
    single {
        S3Config(
            endpoint = System.getenv("S3_ENDPOINT") ?: "http://localhost:9000",
            region = System.getenv("S3_REGION") ?: "us-east-1",
            accessKey = System.getenv("S3_ACCESS_KEY") ?: "minioadmin",
            secretKey = System.getenv("S3_SECRET_KEY") ?: "minioadmin",
            bucket = System.getenv("S3_BUCKET") ?: "casino-engine"
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
}
