# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

**casino-engine** — Kotlin/Ktor microservice serving as the casino game engine for the IGambling platform. Manages game catalog, sessions, betting rounds/spins, and aggregator integrations.

Part of the IGambling platform — see the parent `CLAUDE.md` at `/IGambling/CLAUDE.md` for full platform context.

## Build Commands

```bash
./gradlew build                                    # Build (also runs installDist)
./gradlew test                                     # Run full Kotest suite (JUnit 5 platform)
./gradlew test --tests "domain.service.SpinBalanceCalculatorTest"  # Single spec
./gradlew test --rerun-tasks                       # Force re-run (test task caches results)
./gradlew run                                      # Run application (HTTP :8080, gRPC :5050)
./gradlew generateProto                            # Generate gRPC stubs from proto files
./gradlew runSync                                  # Run aggregator sync CLI locally
```

Test framework: **Kotest 5.9.1** (`FunSpec`) on the JUnit 5 platform, with mockk and kotlinx-coroutines-test. Testcontainers deps are wired but repository/integration tests are not yet written.

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

Four layers: `api/` (gRPC services + REST webhooks) → `application/` (commands/queries, use cases, application ports, events, projections) → `domain/` (models, value objects, factories, repositories, exceptions, events) → `infrastructure/` (adapters, persistence, aggregators, messaging).

### Package layout

```
domain/
├── model/         # Aggregates, entities
├── vo/            # Value objects (@JvmInline value class with init validation)
├── service/       # Domain services (SpinBalanceCalculator, factories)
├── event/         # DomainEvent sealed hierarchy + AggregateRoot + WithEvents
├── repository/    # Repository INTERFACES (DDD-pure: contracts live with the model)
├── exception/     # Sealed DomainException hierarchy
└── util/          # Trait interfaces (Activatable, Imageable, Orderable)

application/
├── Bus.kt                 # CQRS bus contract
├── IHandler.kt            # CqrsHandler marker, ICommand, IQuery, ICommandHandler, IQueryHandler
├── HandlerRegistry.kt     # Reflective registry — auto-discovery
├── command/<feature>/     # Write-side: command DTOs (no handlers — those live in infra)
├── query/<feature>/       # Read-side: query DTOs + their View result types side-by-side
├── usecase/               # Application services / use cases (orchestrators)
├── port/external/         # Driven ports for external systems (FileAdapter, IWalletPort, IEventPort, ...)
└── port/factory/          # Driven ports for adapter factories (AggregatorAdapterProvider, IAggregatorFactory)

infrastructure/
├── handler/<feature>/     # Command/query handler IMPLEMENTATIONS (touch DB / repos / external)
├── persistence/           # Exposed repositories implementing domain.repository contracts
├── aggregator/<vendor>/   # Aggregator integration adapters
├── rabbitmq/              # Event publisher + consumers
├── redis/                 # Player limit cache
├── s3/                    # File storage adapter
├── wallet/                # Wallet gRPC client
└── koin/                  # DI module wiring

api/
├── grpc/service/          # gRPC service implementations (call Bus)
├── grpc/mapper/           # Proto ↔ domain mappers
└── rest/                  # REST webhook routes
```

**Key DDD invariants:**
- **Repository interfaces live in `domain/repository/`** — they are part of the ubiquitous language and the domain model. Implementations are in `infrastructure/persistence/repository/`. The application layer never depends on infrastructure for write paths; it depends on the domain port.
- **Commands and queries are pure data classes** in `application/command/<feature>/` and `application/query/<feature>/`. They contain no logic — all behavior lives in their handler in `infrastructure/handler/<feature>/`.
- **Query result types live next to the query** as top-level data classes in the same file (e.g. `CollectionView` in `FindCollectionQuery.kt`, `LastWin` in `LastWinnerQuery.kt`). When a single read shape is shared across `Find` and `FindAll` for the same feature, the `Find*Query.kt` file owns the type and `FindAll*Query.kt` references it via the same package — there is no separate `projection/` package.
- **Use cases** in `application/usecase/` are orchestrators that take domain models, call repositories + ports, and publish domain events. They are called from command handlers (via `bus.invoke(...)` or directly).

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

`Bus` dispatches via `BusImpl` → `HandlerRegistry`. The registry is populated **automatically** at boot: Koin's `getAll<CqrsHandler>()` surfaces every handler, and `HandlerRegistry.register` pulls the `C`/`Q` generic type argument via Kotlin reflection. Polymorphic handlers (e.g. `SetImageCommandHandler : ICommandHandler<SetImageCommand, Unit>`) also serve concrete subtypes — lookup walks the class hierarchy when an exact match is missed, and the result is cached per concrete class.

**All write paths go through repositories.** Every `SaveXCommandHandler` loads the FK parent (e.g., `IProviderRepository.findByIdentity`), builds/merges a domain aggregate, and calls `repository.save(...)`. There are no `Exposed Table` writes inside handlers — direct writes are confined to `infrastructure/persistence/repository/*Impl`. Repository **interfaces** are defined in `domain/repository/`; **implementations** in `infrastructure/persistence/repository/`.

**Key difference**: Commands return `Result<R>` (wrapped in `runCatching`). Queries return `R` directly.

**Adding a new command/query**:
1. Define the DTO in `application/command/<feature>/X.kt` or `application/query/<feature>/X.kt`
2. Create the handler in `infrastructure/handler/<feature>/XHandler.kt`
3. Bind it with `single(named("x")) { XHandler(...) } bind CqrsHandler::class` in `HandlerModule`

That's it — `BusModule` never needs to be edited.

**Exception helpers**: `domainRequireNotNull(value) { ExceptionType() }` and `domainRequire(condition) { ExceptionType() }` throw categorized `DomainException` subclasses. Handlers and repositories **must not** throw `IllegalArgumentException` or raw `error(...)` for business rule violations — always pick an appropriate `DomainException` subclass so the gRPC interceptor maps to the right status code.

## Data Flow — Spin Lifecycle

1. **OpenSessionUsecase** — aggregator creates game adapter → gets launch URL via `getLaunchUrl(session, lobbyUrl)` → saves session → publishes `SessionOpened` domain event through `DomainEventPublisher`
2. **ProcessSpinUsecase** — for each spin (PLACE/SETTLE/ROLLBACK):
   - Freespin rounds skip balance calculation entirely
   - Regular rounds: check player limits → calculate balance via `SpinBalanceCalculator` → withdraw/deposit via `IWalletPort` → save spin → publish `SpinPlaced`/`SpinSettled`/`SpinRolledBack` via `spin.toDomainEvent()`
3. **FinishRoundUsecase** — `round.finish()` returns `WithEvents<Round>` containing `RoundFinished` → save → publish events via `DomainEventPublisher.publishAll(...)`

Use cases are callable via `operator fun invoke()`, return `Result<Response>`, and take domain models (not DTOs). They inject `DomainEventPublisher` rather than `IEventPort` directly.

**Session convenience**: `session.openRound(externalId, freespinId)` is the preferred way to create a round — it delegates to `RoundFactory` but keeps the call anchored to the parent aggregate.

## Persistence

Exposed ORM wrapped by two helpers in `infrastructure/persistence/DbTransaction.kt`:

- `dbTransaction { }` — suspended write transaction (preferred over `newSuspendedTransaction` direct)
- `dbRead { }` — read-only transaction for query handlers and `find*` repository methods

Nothing outside `DbTransaction.kt` should import `newSuspendedTransaction` directly.

Entity ↔ domain conversion via mapper extension functions (`object XMapper { fun XEntity.toDomain(): X }`). ResultRow extensions use distinct names (`toProvider`, `toAggregator`, etc.) to avoid `toDomain` collisions when one mapper composes another — see `.claude/rules/mapper-conventions.md`.

- **Long PK tables** (`LongIdTable`): SessionTable, RoundTable, SpinTable, GameTable, GameVariantTable, ProviderTable, AggregatorTable, CollectionTable, GameCollectionTable, GameFavouriteTable
- **New entity detection**: `id == Long.MIN_VALUE` means unsaved
- **JSON columns**: `config`, `tags`, `images`, `locales`, `platforms`, `name` (LocaleName) via `kotlinx.serialization`

Repository methods raise domain exceptions on FK violations (`ProviderNotFoundException`, `AggregatorNotFoundException`, `GameNotFoundException`, `CollectionNotFoundException`) via `domainRequireNotNull`. Image updates flow through `IGameRepository.addImage(identity, key, url)` (and the analogous methods on provider/collection) — handlers do not touch entity DAOs directly.

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

Routing is handled by `AggregatorRegistry : IAggregatorFactory` (in `infrastructure/aggregator/`). It indexes every bound `AggregatorAdapterProvider` by its `integration` string and raises `AggregatorNotSupportedException` for unknown keys. Each aggregator provides: Config, `*AdapterProvider` (implementing `AggregatorAdapterProvider` — replaces the old `*AdapterFactory`), GameAdapter (IGamePort), FreespinAdapter (IFreespinPort), HttpClient, and Webhook.

**Adding a new aggregator is one new file + one Koin line.** Create `<Name>AdapterProvider` with an `integration` string and factory methods, then in `ExternalModule`:
```kotlin
single(named("<name>")) { <Name>AdapterProvider() } bind AggregatorAdapterProvider::class
```
The registry picks it up through `getAll<AggregatorAdapterProvider>()` at boot — no edits to `AggregatorRegistry` or any existing code.

See `.claude/skills/add-aggregator.md` for the step-by-step guide. See `.claude/agents/seed-collections.md` for the collection seeding agent.

**Pragmatic specifics**: Uses MD5 hash authentication (sorted params + secret key), form-encoded POST requests, GET webhook endpoints at `/pragmatic/*.html`, and decimal string amounts (converted to/from minor units via ×100).

**Pateplay specifics**: Static game catalog (no game discovery API), launch URLs constructed locally (no API call), HMAC-SHA256 authentication for freespin API, no webhook handler (wallet callback not yet implemented).

## Event System (DomainEvent → RabbitMQ)

**One sealed hierarchy:** `domain/event/DomainEvent` with concrete types `SessionOpened`, `SpinPlaced`, `SpinSettled`, `SpinRolledBack`, `RoundFinished`. Aggregates raise events during state transitions — mutable aggregates extend `AggregateRoot` and call `raise(event)`; immutable data classes return `WithEvents<T>(value, events)` from mutators (e.g., `Round.finish()`). The `Spin.toDomainEvent()` extension maps spin type → event for usecase publishing.

**No translation layer.** `IEventPort.publish(event: DomainEvent)` takes domain events directly. Usecases inject `IEventPort` and call it themselves — there is no `DomainEventPublisher`/`ApplicationEvent` middleman.

**`RabbitMqEventPublisher`** (implements `IEventPort`) does the routing in one place via an exhaustive `when (event: DomainEvent)`:

```kotlin
override suspend fun publish(event: DomainEvent) {
    val (routingKey, payload) = when (event) {
        is SessionOpened  -> SessionOpenedMapper.ROUTING_KEY  to SessionOpenedMapper.toPayload(event)
        is SpinPlaced     -> SpinPlacedMapper.ROUTING_KEY     to SpinPlacedMapper.toPayload(event)
        is SpinSettled    -> SpinSettledMapper.ROUTING_KEY    to SpinSettledMapper.toPayload(event)
        is SpinRolledBack -> SpinRolledBackMapper.ROUTING_KEY to SpinRolledBackMapper.toPayload(event)
        is RoundFinished  -> RoundFinishedMapper.ROUTING_KEY  to RoundFinishedMapper.toPayload(event)
    }
    ...
}
```

Adding a new domain event → compiler forces you to add a `when` branch (sealed exhaustiveness). One mapper file per event type lives in `infrastructure/rabbitmq/mapper/`, each owning its own `<X>Payload` data class + `ROUTING_KEY` constant + `toPayload(...)` function.

**Consumer**: `PlaceSpinEventConsumer` subscribes to the `spin.placed` routing key and enforces player betting limits by updating max place amounts via `IPlayerLimitPort` (Redis). Both `RabbitMqEventPublisher` and `PlaceSpinEventConsumer` depend on `Application` from Koin — this is why `SyncJob` overrides `IEventPort` with a no-op `IEventPort` implementation.

**Publishing timing**: usecases publish **after** the DB transaction commits (outside the `dbTransaction { }` block) so a failed write never emits phantom events. See `.claude/rules/domain-events.md`.

## Koin Dependency Injection

**Module install order matters** — dependencies must be installed before dependents.

**Main server** (`infrastructure/koin/KoinInit.kt`): Registers `Application` instance first, then 8 modules:
`configModule → persistenceModule → externalModule → usecaseModule → handlerModule → busModule → aggregatorModule → grpcModule`

The `grpcModule` is defined in `api/grpc/config/` and registers gRPC service singletons. All other modules are in `infrastructure/koin/`.

**`HandlerModule`**: every handler is declared as `single(named("<x>")) { ... } bind CqrsHandler::class`. The named qualifier is required because Koin rejects duplicate `single`s of the same type when binding to a common supertype; the marker binding is what allows `busModule` to `getAll<CqrsHandler>()` in one call.

**`BusModule`**: tiny (~13 lines). It constructs a `HandlerRegistry`, populates it from `getAll<CqrsHandler>()`, and wraps the result in `BusImpl`. Never needs to be touched when adding handlers.

**`ExternalModule`**: `AggregatorAdapterProvider`s are bound with named qualifiers and `bind AggregatorAdapterProvider::class` so `AggregatorRegistry(providers = getAll())` collects them all. (Note: there is no `ImageAttachmentService` — `SetImageCommandHandler` calls `FileAdapter.upload(...)` directly. There is no `DomainEventPublisher` either — usecases inject `IEventPort` directly.)

**SyncJob** (SyncJob.kt): Same modules minus `grpcModule`, no `Application` registration, includes `syncOverrideModule` for no-op `IEventPort`.

**Application registration**: `RabbitMqEventPublisher` and `PlaceSpinEventConsumer` depend on `io.ktor.server.application.Application` via Koin `get()`. The `Application` instance is explicitly registered in `KoinInit.kt` as `module { single { application } }` because koin-ktor 4.0.3 does not auto-register it.

## Key Design Decisions

- **Value objects**: `@JvmInline value class` with `init` block validation via `domainRequire(...)`; validation errors are `DomainException` subclasses, not `IllegalArgumentException`
- **Amount**: wraps `Long` in minor units (cents) with operator overloads; `Amount.ZERO` constant; `minOf(Amount, Amount)` top-level helper
- **Domain traits** (Activatable, Imageable, Orderable): mutable interfaces. Game overrides via `copy()` for immutability; Provider/Collection/Aggregator mutate directly
- **Monetary values**: `Long` in minor units internally, `string` in proto for BigInteger precision
- **Factories**: `object` singletons with validation (e.g., `SessionFactory.create()` checks active status and locale/platform support); `Session.openRound()` delegates to `RoundFactory` as a convenience on the parent aggregate
- **SpinBalanceCalculator**: PLACE deducts (real-first when bonusBet), SETTLE deposits to same pool as original bet, ROLLBACK refunds to original pools. Pre-checks `canAfford` for every spin type. Exhaustively unit-tested.
- **Spin convenience**: `spin.isPlace` / `isSettle` / `isRollback` computed properties. `Spin.toDomainEvent()` extension maps type → `SpinPlaced`/`SpinSettled`/`SpinRolledBack`
- **Round.finish()**: returns `WithEvents<Round>` carrying a `RoundFinished` domain event — the usecase drains it and publishes after commit
- **Read-side projections**: query handlers that join across aggregates return `application/projection/<ctx>/<X>Projection` DTOs (e.g. `CollectionProjection` with game counts), never polluting domain models with denormalized fields
- **Wallet dependency**: wallet proto resolved via direct `srcDir("../wallete-engine/proto")` source reference in `build.gradle.kts` (note the "wallete" spelling — intentional carve-out, do NOT fix)
- **File storage interface**: Named `FileAdapter` (not `FilePort`), located in `application/port/external/FilePort.kt` — intentional carve-out, do NOT rename

## CI/CD

GitHub Actions workflow (`publish-grpc-client.yml`) publishes `com.nekgamebling:game-grpc-client` to GitHub Packages on tag push (`v*`) or manual dispatch. Version can be overridden with `-PgrpcClientVersion=x.y.z`.
