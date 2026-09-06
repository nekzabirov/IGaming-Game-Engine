# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**casino-engine** — Kotlin/Ktor service: the casino side of an operator. It keeps the game
catalog (synced from GameHub), launches games through GameHub, and is the operator's wallet
endpoint the hub calls for every money movement. It talks to two upstreams over gRPC —
**GameHub** (every vendor, both products) and **pam-engine** (player, wallet ledger, currencies)
— and publishes events to RabbitMQ for crm-engine.

## Build & run

```bash
./gradlew build                 # compile + tests + installDist
./gradlew test                  # Kotest; integration specs need Docker (Testcontainers Postgres)
./gradlew test --tests "services.WalletServiceTest"
./gradlew run                   # HTTP :8080 (/health only), gRPC :5050
./gradlew runSync               # one-shot catalog sync from GameHub
./gradlew runMigrate            # create DB if missing + Flyway
docker compose up -d postgres rabbitmq redis
```

Three entrypoints, all in the root package: `Application.kt` (server), `SyncJob.kt`
(`bin/sync-catalog`, the daily CronJob), `DbMigrateJob.kt` (`bin/db-migrate`, runs before deploy).
CI (`build-and-push.yml`) runs `assemble -x test` — tests run locally only.

## Layout — a flat Ktor application

```
src/main/kotlin/
├── Application.kt        main() + Application.module(config): explicit wiring, no DI framework
├── AppConfig.kt          every env var, read once
├── plugins/              configureX(): Databases (Hikari + Exposed + dbTransaction/dbRead),
│                         Grpc (server + services), Messaging (exchange + consumer), Routing (/health)
├── db/                   Exposed: Tables, Entities (DAO — the model), Enums, Paging, Search (fuzzy)
├── errors/               DomainException hierarchy + Valid.* input checks
├── clients/              GameHub (gRPC to the hub), Wallet (pam), PlayerLimits (Redis) — each an
│                         interface + one implementation, so services are testable without them
├── events/               Payload classes + wire serializers (FROZEN contract), RabbitEventPublisher,
│                         PlaceSpinEventConsumer
├── services/             Business logic, one class per concern: Game/Provider/Collection/Round/Winner
│                         (catalog reads & writes), Launch (hub launches), Wallet (money path),
│                         CatalogSync, SpinMath (pure split arithmetic), GameFilter (proto filter → SQL)
├── dto/                  entity → proto mapping, run inside the transaction that loaded the entity
└── grpc/                 one class per gRPC service: validate → call a service → wrap the result
```

Dependency direction: `grpc → services → db/clients/events`; `dto` is used by services (proto is
the app's response model). There is no domain/application/infrastructure split and no CQRS bus —
do not reintroduce them.

## Rules that matter

- **Entities are the model.** `db.CasinoGame`, `CasinoProvider`, `Collection`, `CasinoRound`,
  `Spin` are Exposed DAO classes; there is no second set of model classes and no mappers between
  them. Bulk work (catalog sync) uses the DSL (`batchUpsert`) on the same tables.
- **Entities never leave a transaction.** Relations are lazy. Read what you need inside
  `dbTransaction {}` / `dbRead {}` and return plain values, a proto DTO (`dto/`) or an event
  payload (`events/Payloads.kt`).
- **No remote call inside a transaction.** pam, GameHub, Redis and RabbitMQ are called between
  transactions, never within one. The money path is: tx (find/open round) → wallet → tx (insert
  spin) → publish. `plugins/Databases.kt` documents why.
- **Events are a public contract.** `events/Events.kt` defines the exact wire shape (field names,
  nesting, the legacy `session`/`gameVariant` twins, flat `gameIdentity`/`gameProvider`/`currency`/
  `freespinId`). Routes `spin.events`, `round.events`, `session.events`; envelope
  `{ "playerId", "data" }`; exchange from `EVENT_EXCHANGE`. The consumer queue name
  `PlaceSpinEventConsumer` is durable on every broker — never rename it. Publish only after the
  transaction committed; a publish failure is logged, never surfaced to the hub.
- **Exception class names are on the wire.** `handleGrpcCall` puts the simple name in the
  `x-exception-name` trailer (casino-app switches on `CasinoGameUnavailableException`) and maps the
  category to a status: NotFound → NOT_FOUND, BadRequest → INVALID_ARGUMENT, Conflict →
  ALREADY_EXISTS, Forbidden → PERMISSION_DENIED, System → INTERNAL. Keep names when refactoring.
- **The hub's wallet contract** (`grpc/WalletGrpcService.kt`): authenticated by
  `x-operator-id`/`x-operator-key` = `GAMEHUB_OPERATOR_ID`/`KEY`; refusals carry `x-error-code`
  (`INSUFFICIENT_FUNDS`, `LIMIT_EXCEEDED`, `PLAYER_NOT_FOUND`, `INTERNAL`). `INTERNAL` means
  "unknown outcome" and starts a vendor rollback cycle — never use it for a refusal on the merits.
- **Idempotency lives in Postgres.** `spins.external_id` is unique (the hub's leg id);
  `casino_rounds.external_id` is unique (find-or-create by upsert). A redelivered leg answers the
  current balance and moves nothing; the wallet reference `spin:<type>:<leg id>` makes pam
  idempotent too.
- **Catalog ownership.** The sync overwrites everything the hub reports (`name`, `tags`, `images`,
  capability flags, locales, platforms, `rtp` — kept when the hub reports none). Local-only:
  `active`, `order`, bonus flags, `blocked_country`, `custom_images`, `custom_tags`. The wire shows
  `images`/`tags` merged (local wins per key / appended); `custom_tags` is exposed so an operator
  can edit the local half. `rtp = 0` on the wire means unmeasured.
- **Search** (`db/Search.kt`) mirrors the expression indexes of `V11__fuzzy_search.sql`; change
  both sides together. Strict pass first, Levenshtein fallback only on zero hits.
- **Schema is Flyway's** (`src/main/resources/db/migration`); `db/Tables.kt` only mirrors it.
- **Proto changes** require updating `src/main/proto/API.md` (a Stop hook enforces it).

## Money path cheat-sheet (`services/WalletService.kt`)

PLACE withdraws (real first, then bonus unless the game forbids bonus bets; refused when short or
over the player's cap); SETTLE deposits into the pool the round's bonus bet came from; ROLLBACK
moves opposite to the leg it reverses (a win is reclaimed clamped to what is left); a free round's
PLACE never touches the wallet and its SETTLE only when `FREESPIN_TO_PAYOUT=true`. CloseRound
publishes the round finished; a late leg reopens it silently. An unknown game does not refuse
money — the round opens without one, like a sportsbook leg.

## Config

All env vars: `AppConfig.kt`. Notable: `DB_URL` (base, no db name) + `DATABASE_NAME`,
`DB_POOL_SIZE` (also caps the JDBC dispatcher), `PAM_GRPC_HOST/PORT`, `GAMEHUB_GRPC_HOST/PORT`,
`GAMEHUB_OPERATOR_ID/KEY`, `GAMEHUB_GRPC_PLAINTEXT` (false = TLS, JDK provider — BoringSSL
SIGSEGVs in this image), `REDIS_HOST/PORT`, `RABBIT_*` (+`RABBIT_TLS`), `EVENT_EXCHANGE`,
`FREESPIN_TO_PAYOUT`.

## Tests

Kotest `FunSpec`. Unit: `SpinMath`, wire serializers, publisher (mockk), `Valid`, paging, config.
Integration (`support/TestDatabase`): a Testcontainers Postgres migrated by the real Flyway
scripts; fakes for wallet/limits/hub/events live in `support/Fakes.kt`. Every money rule in the
cheat-sheet has a spec in `WalletServiceTest`.
