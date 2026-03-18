package infrastructure.koin

import infrastructure.persistence.DatabaseConfig
import infrastructure.rabbitmq.RabbitMqConfig
import infrastructure.redis.RedisConfig
import infrastructure.s3.S3Config
import infrastructure.wallet.WalletConfig
import org.koin.dsl.module

val configModule = module {
    single {
        DatabaseConfig(
            url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/game_core",
            user = System.getenv("DATABASE_USER") ?: "postgres",
            password = System.getenv("DATABASE_PASSWORD") ?: "postgres"
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
            uri = System.getenv("RABBITMQ_URL") ?: "amqp://guest:guest@localhost:5672",
            exchange = System.getenv("RABBITMQ_EXCHANGE") ?: "casino-engine"
        )
    }
}
