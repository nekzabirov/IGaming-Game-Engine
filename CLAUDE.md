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

**Note:** No automated tests exist yet. The test commands are listed for when tests are added.

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

Hexagonal Architecture + DDD + CQRS. Kotlin 2.0.21, JDK 21, Ktor 3.0.3 (CIO), Exposed ORM, Koin DI, gRPC + protobuf, RabbitMQ, Redis (Lettuce), AWS S3. Dependency versions managed in `gradle/libs.versions.toml`.

Four layers: `api/` (gRPC services + REST webhooks) → `application/` (CQRS commands/queries, use cases, ports, events) → `domain/` (models, value objects, factories, exceptions) → `infrastructure/` (adapters, persistence, aggregators, messaging).

Ports: HTTP 8080 (dev) / 80 (Docker), gRPC 5050. Configurable via `HTTP_PORT` and `GRPC_PORT` env vars.

## Entrypoints

### Main Server (Main.kt)

Boot sequence (each step is a `configure*()` extension function on `Application`):
1. `configureKoin()` — registers `Application` instance, then installs 8 modules (config → persistence → external → usecase → handler → bus → aggregator → grpc)
2. `configureDatabase()` — initializes Exposed connection pool, creates tables
3. `configureSerialization()` — kotlinx.serialization JSON with `ignoreUnknownKeys`
4. `configureCallLogging()`
5. `configureRabbitMq()` — installs RabbitMQ plugin
6. `configureRouting()` — registers aggregator webhook routes under `/api/webhook` (defined in `api/rest/RestModule.kt`)
7. `configureGrpc()` — launches gRPC server on separate coroutine (IO dispatcher) with 6 services (defined in `api/grpc/GrpcModule.kt`)
8. `configureConsumers()` — starts RabbitMQ event consumers

Environment variables: `HTTP_PORT` (default 8080), `GRPC_PORT` (default 5050).

### Sync Job (SyncJob.kt)

Standalone CLI entrypoint that syncs games from all active aggregators, then exits. Uses `startKoin` directly (not koin-ktor) with same modules minus `grpcModule`, no `Application` registration. Dispatches `SyncAllActiveAggregatorCommand` via the CQRS Bus.

**Important**: SyncJob does NOT register `Application` in Koin. A `syncOverrideModule` is loaded after `externalModule` to replace `IEventPort` with a no-op implementation (sync doesn't publish events). If adding new singletons that depend on `Application`, ensure the sync code path doesn't resolve them, or add an override in `syncOverrideModule`.

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

## Persistence

Exposed ORM with `newSuspendedTransaction`. Entity ↔ domain conversion via mapper extension functions (`object XMapper { fun XEntity.toDomain(): X }`).

- **Long PK tables** (`LongIdTable`): SessionTable, RoundTable, SpinTable, GameTable, GameVariantTable, ProviderTable, AggregatorTable, CollectionTable, GameCollectionTable, GameFavouriteTable
- **New entity detection**: `id == Long.MIN_VALUE` means unsaved
- **JSON columns**: `config`, `tags`, `images`, `locales`, `platforms`, `name` (LocaleName) via `kotlinx.serialization`

See `.claude/rules/exposed-database.md` for detailed Exposed ORM conventions.

## Proto / gRPC

Proto files in `src/main/proto/game/v1/`. Package: `game.v1` (Java: `com.nekgamebling.game.v1`).

- DTOs in `dto/` subdirectory as `<name>.dto.proto` (see `.claude/rules/proto-dto.md`)
- Services in `service/` subdirectory: GameService, CollectionService, ProviderService, AggregatorService, FreespinService, WinnerService
- Full gRPC client API reference: `src/main/proto/API.md`

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

Each aggregator provides: Config, AdapterFactory, GameAdapter (IGamePort), FreespinAdapter (IFreespinPort), HttpClient, and Webhook (Ktor Route handler). See `.claude/skills/add-aggregator.md` for the step-by-step guide. See `.claude/agents/seed-collections.md` for the collection seeding agent.

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

**Main server** (`infrastructure/koin/KoinInit.kt`): Registers `Application` instance first, then 8 modules:
`configModule → persistenceModule → externalModule → usecaseModule → handlerModule → busModule → aggregatorModule → grpcModule`

The `grpcModule` is defined in `api/grpc/config/` and registers gRPC service singletons. All other modules are in `infrastructure/koin/`.

**SyncJob** (SyncJob.kt): Same modules minus `grpcModule`, no `Application` registration, includes `syncOverrideModule` for no-op `IEventPort`.

**Application registration**: `RabbitMqEventPublisher` and `PlaceSpinEventConsumer` depend on `io.ktor.server.application.Application` via Koin `get()`. The `Application` instance is explicitly registered in `KoinInit.kt` as `module { single { application } }` because koin-ktor 4.0.3 does not auto-register it.

## Key Design Decisions

- **Value objects**: `@JvmInline value class` with `init` block validation via `require()`
- **Amount**: wraps `Long` in minor units (cents) with operator overloads; `Amount.ZERO` constant
- **Domain traits** (Activatable, Imageable, Orderable): mutable interfaces. Game overrides via `copy()` for immutability; Provider/Collection/Aggregator mutate directly
- **Monetary values**: `Long` in minor units internally, `string` in proto for BigInteger precision
- **Factories**: `object` singletons with validation (e.g., `SessionFactory.create()` checks active status and locale/platform support)
- **SpinBalanceCalculator**: PLACE deducts (real-first when bonusBet), SETTLE deposits to same pool as original bet, ROLLBACK refunds to original pools
- **Wallet dependency**: wallet proto resolved via direct `srcDir("../wallete-engine/proto")` source reference in `build.gradle.kts` (note the "wallete" spelling)
- **File storage interface**: Named `FileAdapter` (not `FilePort`), located in `application/port/external/`

## CI/CD

GitHub Actions workflow (`publish-grpc-client.yml`) publishes `com.nekgamebling:game-grpc-client` to GitHub Packages on tag push (`v*`) or manual dispatch. Version can be overridden with `-PgrpcClientVersion=x.y.z`.
