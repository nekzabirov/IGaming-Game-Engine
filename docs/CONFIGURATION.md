# Configuration

Every variable is read once in `AppConfig.kt`.

| Variable | Description | Default |
| --- | --- | --- |
| `HTTP_PORT` | HTTP port (`/health` only) | `8080` |
| `GRPC_PORT` | gRPC port | `5050` |
| `DB_URL` | JDBC base URL, without the database name | `jdbc:postgresql://localhost:5432` |
| `DATABASE_NAME` | Database name, appended to `DB_URL` | `casino` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `user` / `password` |
| `DB_POOL_SIZE` | Hikari pool size; also caps the JDBC dispatcher | `10` |
| `PAM_GRPC_HOST` / `PAM_GRPC_PORT` | pam-engine (player, wallet, currencies), plaintext | `localhost` / `9090` |
| `GAMEHUB_GRPC_HOST` / `GAMEHUB_GRPC_PORT` | GameHub GatewayService | `localhost` / `443` |
| `GAMEHUB_OPERATOR_ID` / `GAMEHUB_OPERATOR_KEY` | Operator pair, sent to the hub and checked on its wallet calls | empty |
| `GAMEHUB_GRPC_PLAINTEXT` | `true` for a local hub over loopback; prod is TLS | `false` |
| `REDIS_HOST` / `REDIS_PORT` | Player limits | `localhost` / `6379` |
| `RABBIT_HOST` / `RABBIT_PORT` / `RABBIT_USER` / `RABBIT_PASSWORD` | Broker | `localhost` / `5672` / `guest` / `guest` |
| `RABBIT_TLS` | `amqps://` (Amazon MQ) | `false` |
| `EVENT_EXCHANGE` | Topic exchange for `spin.events` / `round.events` / `session.events` | `crm.exchange` |
| `FREESPIN_TO_PAYOUT` | Whether this engine credits a free round's win to the wallet | `true` |

## Entrypoints

| Binary | Purpose |
| --- | --- |
| `bin/casino-engine` | server: gRPC + HTTP + RabbitMQ consumer |
| `bin/sync-catalog` | one-shot catalog sync from GameHub (daily CronJob) |
| `bin/db-migrate` | creates the database if missing and applies Flyway migrations |

## Local development

```bash
docker compose up -d postgres rabbitmq redis
./gradlew runMigrate
./gradlew run
```
