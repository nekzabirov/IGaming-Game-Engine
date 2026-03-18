# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

**casino-engine** — Kotlin/Ktor microservice serving as the casino game engine for the IGambling platform. Manages game catalog, sessions, betting rounds/spins, and aggregator integrations.

Part of the IGambling platform — see the parent `CLAUDE.md` at `/IGambling/CLAUDE.md` for full platform context.

## Build Commands

```bash
./gradlew build                # Build (also runs installDist)
./gradlew test                 # Run all tests
./gradlew test --tests "com.nekgamebling.SomeTest"  # Single test
./gradlew run                  # Run application (HTTP :8080, gRPC :5050)
./gradlew generateProto        # Generate gRPC stubs from proto files
./gradlew runSync              # Run aggregator sync CLI locally
```

## Local Development

```bash
# 1. Start infrastructure
docker-compose up -d postgres rabbitmq redis minio minio-init

# 2. Configure environment
cp .env.example .env           # Defaults point to localhost

# 3. Run the application
./gradlew run                  # Starts HTTP on :8080, gRPC on :5050
```

Full stack (infra + app in Docker):
```bash
./gradlew build                # Creates build/distributions/casino-engine-*.tar
docker-compose up -d
```

Two application entrypoints in Docker:
- `/app/bin/casino-engine` — main server (HTTP + gRPC + consumers)
- `/app/bin/sync-aggregators` — one-shot aggregator game sync job

## Architecture

Hexagonal Architecture + DDD + CQRS.

```
src/main/kotlin/
├── api/
│   ├── grpc/                  # gRPC entry points
│   │   ├── config/            # GrpcExceptionInterceptor (handleGrpcCall), GrpcModule (Koin)
│   │   ├── mapper/            # Domain→Proto mappers
│   │   └── service/           # gRPC service impls (Game, Provider, Collection, Aggregator, Freespin)
│   └── rest/                  # REST endpoints (aggregator webhooks registered via Main.kt routing)
├── application/
│   ├── cqrs/                  # ICommand<R>, IQuery<R>, Bus, organized by domain
│   ├── event/                 # ApplicationEvent sealed interface (SpinEvent, RoundEndEvent, SessionOpenEvent)
│   ├── port/                  # Port interfaces: storage/ (repositories) + external/ (services) + factory/
│   └── usecase/               # Orchestrators: OpenSession, ProcessSpin, FinishRound, SyncAggregator
├── domain/
│   ├── exception/             # DomainException hierarchy (notfound/, badrequest/, conflict/, forbidden/)
│   ├── model/                 # Aggregates: Game, GameVariant, Session, Round, Spin, Provider, Collection, Aggregator
│   ├── service/               # SessionFactory, RoundFactory, SpinFactory, SpinBalanceCalculator
│   ├── util/                  # Mutable traits: Activatable, Imageable, Orderable
│   └── vo/                    # Value objects: Identity, Currency, Locale, Amount, PlayerId, SessionToken, etc.
└── infrastructure/
    ├── aggregator/            # Aggregator adapters (OneGameHub, Pragmatic, Pateplay)
    ├── handler/               # CQRS handler implementations organized by domain
    ├── koin/                  # DI modules (Config, Persistence, External, Usecase, Handler, Bus, Aggregator)
    ├── persistence/           # Exposed ORM: table/, entity/, mapper/, repository/
    ├── rabbitmq/              # RabbitMqEventPublisher + PlaceSpinEventConsumer + event mappers
    ├── redis/                 # PlayerLimitRedis
    ├── s3/                    # S3FileAdapter
    ├── unit/                  # CurrencyAdapter, BackgroundWorker
    └── wallet/                # WalletAdapter (gRPC client to wallet-service)
```

## Entrypoints

### Main Server (Main.kt)

Boot sequence:
1. Koin DI — registers `Application` instance, then installs 8 modules (config → persistence → external → usecase → handler → bus → aggregator → grpc)
2. Database — initializes Exposed connection pool, creates tables
3. Serialization — kotlinx.serialization JSON with `ignoreUnknownKeys`
4. Call logging
5. HTTP routing — registers aggregator webhook routes (OneGameHub, Pragmatic)
6. gRPC server — launches on separate coroutine (IO dispatcher) with 5 services
7. Consumers — starts RabbitMQ event consumers

### Sync Job (SyncJob.kt)

Standalone CLI entrypoint that syncs games from all active aggregators, then exits. Uses `startKoin` directly (not koin-ktor) with 8 modules (no gRPC module, no Application registration). Dispatches `SyncAllActiveAggregatorCommand` via the CQRS Bus.

**Important**: SyncJob does NOT register `Application` in Koin. A `syncOverrideModule` is loaded after `externalModule` to replace `IEventPort` with a no-op implementation (sync doesn't publish events). `PlaceSpinEventConsumer` is still registered but never resolved during sync. If adding new singletons that depend on `Application`, ensure the sync code path doesn't resolve them, or add an override in `syncOverrideModule`.

## CQRS Pattern

`Bus` dispatches commands and queries to handlers via class-to-handler maps wired in `BusModule`.

**Two handler styles coexist:**

- **Repository-based** (domain logic needed): Handler calls repository port → domain model → usecase. Session handlers follow this: find session by token → find/create round → invoke usecase.
- **Entity-direct** (simple CRUD): Handler works directly with Exposed Table/Entity inside `newSuspendedTransaction`. Examples: `SaveAggregatorCommandHandler`, `SaveProviderCommandHandler`.

**Key difference**: Commands return `Result<R>` (wrapped in `runCatching`). Queries return `R` directly.

**Adding a new command/query**: Define in `application/cqrs/<domain>/`, create handler in `infrastructure/handler/<domain>/`, register handler singleton in `HandlerModule`, wire command→handler mapping in `BusModule`.

**Exception helpers**: `domainRequireNotNull(value) { ExceptionType() }` and `domainRequire(condition) { ExceptionType() }` throw categorized `DomainException` subclasses.

## Data Flow — Spin Lifecycle

1. **OpenSessionUsecase** — aggregator creates game adapter → gets launch URL → saves session → publishes `SessionOpenEvent`
2. **ProcessSpinUsecase** — for each spin (PLACE/SETTLE/ROLLBACK):
   - Freespin rounds skip balance checks entirely
   - Regular rounds: check player limits → calculate balance via `SpinBalanceCalculator` → withdraw/deposit via `IWalletPort` → save spin → publish `SpinEvent`
3. **FinishRoundUsecase** — marks round finished → publishes `RoundEndEvent`

Use cases are callable via `operator fun invoke()`, return `Result<Response>`, and take domain models (not DTOs).

## Port Interfaces (application/port/)

**Repository ports** (storage/): `ISessionRepository`, `IRoundRepository`, `ISpinRepository`, `IGameRepository`, `IGameVariantRepository`, `IProviderRepository`, `ICollectionRepository`, `IAggregatorRepository`

**External service ports** (external/):
- `IWalletPort` — balance, withdraw, deposit → `WalletAdapter` (wallet-service gRPC)
- `IGamePort` — aggregator game operations: `getAggregatorGames()`, `getDemoUrl()`, `getLunchUrl()`
- `IFreespinPort` — freespin preset, create, cancel
- `IPlayerLimitPort` — max place amount enforcement → `PlayerLimitRedis`
- `ICurrencyPort` — unit conversion → `CurrencyAdapter`
- `IEventPort` — publish domain events → `RabbitMqEventPublisher`
- `IBackgroundTaskPort` — fire-and-forget coroutine launcher → `BackgroundWorker`
- `FilePort` — file upload/storage → `S3FileAdapter`

**Factory port**: `IAggregatoryFactory` — creates `IGamePort`/`IFreespinPort` per aggregator → `AggregatorFabricImpl`

## Persistence

Exposed ORM with `newSuspendedTransaction`. Entity ↔ domain conversion via mapper extension functions (`object XMapper { fun XEntity.toDomain(): X }`).

- **Long PK tables** (`LongIdTable`): SessionTable, RoundTable, SpinTable, GameTable, GameVariantTable, ProviderTable, AggregatorTable, CollectionTable, GameCollectionTable, GameFavouriteTable
- **New entity detection**: `id == Long.MIN_VALUE` means unsaved
- **JSON columns**: `config`, `tags`, `images`, `locales`, `platforms`, `name` (LocaleName) via `kotlinx.serialization`

See `.claude/rules/exposed-database.md` for detailed Exposed ORM conventions.

## Proto / gRPC

Proto files in `src/main/proto/game/v1/`. Package: `game.v1` (Java: `com.nekgamebling.game.v1`).

- DTOs in `dto/` subdirectory as `<name>.dto.proto` (see `.claude/rules/proto-dto.md`)
- Services in `service/` subdirectory: GameService, CollectionService, ProviderService, AggregatorService, FreespinService

Each gRPC service extends `*GrpcKt.*CoroutineImplBase`, takes `Bus` as constructor parameter, and wraps every method in `handleGrpcCall { }` which maps `DomainException` → gRPC status codes and stores the exception class name in an `x-exception-name` metadata header for downstream error identification.

**Exception → Status mapping**: `NotFoundException` → `NOT_FOUND`, `BadRequestException` → `INVALID_ARGUMENT`, `ConflictException` → `ALREADY_EXISTS`, `ForbiddenException` → `PERMISSION_DENIED`, `SystemException` → `INTERNAL`.

**Name collision**: Proto and CQRS classes share names (e.g., `SaveGameCommand`). Use Kotlin import aliases:
```kotlin
import com.nekgamebling.game.v1.SaveGameCommand as SaveGameProto
import application.cqrs.game.SaveGameCommand as SaveGameCqrs
```

## Aggregator Integration

Currently implemented: **OneGameHub** (`infrastructure/aggregator/onegamehub/`), **Pragmatic Play** (`infrastructure/aggregator/pragmatic/`), and **Pateplay** (`infrastructure/aggregator/pateplay/`).

`AggregatorFabricImpl` routes by `aggregator.integration` string (e.g., `"ONEGAMEHUB"`, `"PRAGMATIC"`, `"PATEPLAY"`) to create the appropriate `IGamePort`/`IFreespinPort` adapters.

Each aggregator provides: Config, AdapterFactory, GameAdapter (IGamePort), FreespinAdapter (IFreespinPort), HttpClient, and Webhook (Ktor Route handler). See `.claude/skills/add-aggregator.md` for the step-by-step guide.

**Pragmatic specifics**: Uses MD5 hash authentication (sorted params + secret key), form-encoded POST requests, GET webhook endpoints at `/pragmatic/*.html`, and decimal string amounts (converted to/from minor units via ×100).

**Pateplay specifics**: Static game catalog (no game discovery API), launch URLs constructed locally (no API call), HMAC-SHA256 authentication for freespin API, no webhook handler (wallet callback not yet implemented).

## Event System (RabbitMQ)

**Event types** (sealed interface `ApplicationEvent`):
- `SessionOpenEvent` → routing key `session.opened`
- `SpinEvent` → routing key `spin.placed`, `spin.settled`, or `spin.rollback` (based on spin type)
- `RoundEndEvent` → routing key `round.finished`

`RabbitMqEventPublisher` (implements `IEventPort`) publishes domain events to the configured RabbitMQ exchange. Event mappers in `infrastructure/rabbitmq/mapper/` transform domain events to JSON payloads.

**Consumer**: `PlaceSpinEventConsumer` subscribes to `spin.placed` events and enforces player betting limits by updating max place amounts via `IPlayerLimitPort` (Redis). Both `RabbitMqEventPublisher` and `PlaceSpinEventConsumer` depend on `Application` from Koin — this is why `SyncJob` overrides `IEventPort` with a no-op.

## Koin Dependency Injection

**Module install order matters** — dependencies must be installed before dependents.

**Main server** (KoinInit.kt): Registers `Application` instance first, then 8 modules:
`configModule → persistenceModule → externalModule → usecaseModule → handlerModule → busModule → aggregatorModule → grpcModule`

**SyncJob** (SyncJob.kt): Uses `startKoin` directly with 8 modules (no grpcModule, no Application registration; includes `syncOverrideModule` for no-op `IEventPort`).

**Application registration**: `RabbitMqEventPublisher` and `PlaceSpinEventConsumer` depend on `io.ktor.server.application.Application` via Koin `get()`. The `Application` instance is explicitly registered in `KoinInit.kt` as `module { single { application } }` because koin-ktor 4.0.3 does not auto-register it.

## Key Design Decisions

- **Value objects**: `@JvmInline value class` with `init` block validation via `require()`
- **Amount**: wraps `Long` in minor units (cents) with operator overloads; `Amount.ZERO` constant
- **Domain traits** (Activatable, Imageable, Orderable): mutable interfaces. Game overrides via `copy()` for immutability; Provider/Collection/Aggregator mutate directly
- **Monetary values**: `Long` in minor units internally, `string` in proto for BigInteger precision
- **Factories**: `object` singletons with validation (e.g., `SessionFactory.create()` checks active status and locale/platform support)
- **SpinBalanceCalculator**: PLACE deducts (real-first when bonusBet), SETTLE deposits to same pool as original bet, ROLLBACK refunds to original pools
- **Wallet dependency**: wallet-grpc-client resolved via Gradle `includeBuild` from `../wallete-engine/wallet-grpc-client` (note the "wallete" spelling)
- **Dependency versions**: Managed via Gradle version catalog in `gradle/libs.versions.toml`

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0.21, JDK 21 |
| Server | Ktor 3.0.3 (CIO engine) |
| ORM | Exposed 0.57.0 + PostgreSQL |
| DI | Koin 4.0.3 |
| gRPC | io.grpc 1.68.2, protobuf 4.29.2 |
| Messaging | RabbitMQ (ktor-server-rabbitmq) |
| Caching | Redis (Lettuce 6.5.3) |
| Storage | AWS S3 SDK for Kotlin |
| Testing | kotlin-test, MockK, kotlinx-coroutines-test |

## Ports

- HTTP: 8080 (dev) / 80 (Docker)
- gRPC: 5050

## CI/CD

GitHub Actions workflow (`publish-grpc-client.yml`) publishes `com.nekgamebling:game-grpc-client` to GitHub Packages on tag push (`v*`) or manual dispatch. Version can be overridden with `-PgrpcClientVersion=x.y.z`.
