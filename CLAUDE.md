# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate gRPC stubs from .proto files
./gradlew generateProto

# Build gRPC client JAR (for external service consumers)
./gradlew grpcClientJar

# Run the application
./gradlew run
```

## Architecture Overview

This is a **Kotlin iGambling Core Service** following **Hexagonal Architecture** (Ports & Adapters) with DDD patterns.

### Layer Structure

```
src/main/kotlin/
├── application/           # Use cases, services, sagas (orchestration)
│   ├── port/outbound/    # Adapter interfaces (WalletAdapter, PlayerLimitAdapter, CacheAdapter)
│   ├── saga/spin/        # Distributed transaction sagas (PlaceSpinSaga, SettleSpinSaga, etc.)
│   └── usecase/          # Application use cases organized by domain
│
├── domain/               # Pure business logic, no external dependencies
│   ├── */                # Bounded contexts: session, game, provider, collection, aggregator
│   └── common/           # Shared: DomainError sealed classes, events, value objects
│
├── infrastructure/       # Technical implementations
│   ├── aggregator/       # Game aggregator integrations (pragmatic, onegamehub, pateplay)
│   ├── api/grpc/         # gRPC service implementations
│   ├── messaging/        # RabbitMQ event publishing
│   └── persistence/      # Exposed ORM repositories
│
└── shared/               # Extensions, serializers, common value objects
```

### Key Architectural Patterns

**Saga Pattern for Spin Operations**: All betting operations (place, settle, end, rollback) use sagas for distributed transactions with automatic compensation on failure. Each saga is in `application/saga/spin/` with dedicated step files.

- `PlaceSpinSaga` (6 steps): validates game → creates round → validates balance → withdraws → saves spin → publishes event
- `SettleSpinSaga` (6 steps): finds round → finds spin → calculates amounts → deposits → saves settle → publishes event
- `EndSpinSaga` (3 steps): finds round → marks finished → publishes event
- `RollbackSpinSaga` (5 steps): finds round → finds original spin → refunds → saves rollback → publishes event

**Aggregator Factory Pattern**: Each game aggregator (Pragmatic, OneGameHub, Pateplay) has its own module in `infrastructure/aggregator/` with:
- `*AdapterFactory` - creates port implementations
- `*Handler` - processes aggregator callbacks using sagas
- `*Config` - aggregator-specific configuration

**Domain Errors**: Type-safe error handling via sealed classes in `domain/common/error/`. Errors include: `NotFoundError`, `InsufficientBalanceError`, `SpinLimitExceededError`, `SessionInvalidError`, etc.

**Dependency Injection**: Koin modules in `infrastructure/DependencyInjection.kt`. Key modules: `coreModule()`, `adapterModule`, `sagaModule`, `AggregatorModule`.

**Command/Query Handler Pattern**: Application layer uses typed handlers for all operations:
- Commands: `application/port/inbound/*/command/` - Create, Update operations
- Queries: `application/port/inbound/*/query/` - Find, FindAll operations
- Handlers are registered in `infrastructure/handler/handlerModule.kt` with named qualifiers
- gRPC services wire handlers via `infrastructure/api/grpc/grpcModule.kt`

**Proto-to-Handler Flow**:
1. Proto message → gRPC service method (`infrastructure/api/grpc/service/*GrpcService.kt`)
2. Maps to Command/Query data class (`application/port/inbound/`)
3. Handler executes (`infrastructure/handler/`)
4. Result mapped back to Proto response

## Technology Stack

- Kotlin 2.0.21 (JVM 21)
- Ktor Server 3.0.3
- Exposed ORM 0.57.0 (H2 for dev, PostgreSQL for prod)
- Koin 4.0.3 (DI)
- gRPC + Protocol Buffers (API)
- RabbitMQ (messaging)

## Proto Files

gRPC service definitions are in `src/main/proto/`:
- `service/` - Service definitions (Session, Game, Freespin, Collection, Provider, Round, Aggregator, Sync)
- `dto/` - Data transfer objects

After modifying `.proto` files, run `./gradlew generateProto`.

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC database URL | `jdbc:h2:mem:test` |
| `DATABASE_DRIVER` | JDBC driver class | `org.h2.Driver` |
| `DATABASE_USER` | Database username | (empty) |
| `DATABASE_PASSWORD` | Database password | (empty) |
| `GRPC_PORT` | gRPC server port | `5050` |
| `HTTP_PORT` | HTTP server port | `8080` |
| `S3_ENDPOINT` | S3-compatible storage endpoint | (required for images) |
| `S3_ACCESS_KEY` | S3 access key | (required for images) |
| `S3_SECRET_KEY` | S3 secret key | (required for images) |
| `S3_BUCKET` | S3 bucket name | (required for images) |
| `S3_REGION` | S3 region | (required for images) |

## Performance Optimizations

**Balance Caching**: The service includes an in-memory balance cache (`BalanceCache`) with 10-second TTL to reduce redundant wallet HTTP calls during high-frequency betting operations.

**Async Processing**: Wallet withdrawals support async processing with predicted balance for faster response times. Round creation and balance validation run in parallel where possible.

**Repository Pattern**: All persistence uses repository-based access with optimized single-query steps for round and spin handling.

## Required Custom Adapters

The service ships with default adapters. For production, implement:

- `WalletAdapter` - balance queries, withdrawals, deposits, rollbacks
- `PlayerLimitAdapter` - spin limit storage per player (cache-backed implementation provided in `infrastructure/external/CachePlayerLimitAdapter.kt`)
- `CacheAdapter` - session caching
- `FileAdapter` - file storage operations (S3 implementation provided in `infrastructure/external/s3/`)
- `EventPublisherAdapter` - domain event publishing (RabbitMQ implementation provided)

Register custom implementations in `infrastructure/DependencyInjection.kt`.

## Adding a New Aggregator

1. Add enum value to `shared/value/Enums.kt`
2. Create package `infrastructure/aggregator/youraggregator/` with:
   - `model/YourConfig.kt` - configuration class
   - `adapter/YourLaunchUrlAdapter.kt`, `YourFreespinAdapter.kt`, `YourGameSyncAdapter.kt`
   - `YourAdapterFactory.kt` - implements `AggregatorAdapterFactory`
   - `YourHandler.kt` - callback handler using sagas
3. Create Koin module and register in `AggregatorModule`
4. Add REST routes for callbacks

## Event Routing Keys

Events published to RabbitMQ:
- `spin.placed`, `spin.settled`, `spin.end`, `spin.rollback`
- `session.opened`
- `game.favourite.added`, `game.favourite.removed`, `game.won`

## Adding a New gRPC Method

1. Define message types in `src/main/proto/service/*.proto` or `dto/*.proto`
2. Run `./gradlew generateProto`
3. Create Command/Query data class in `application/port/inbound/`
4. Create Handler in `infrastructure/handler/`
5. Register handler in `handlerModule.kt` with `named()` qualifier
6. Add method to gRPC service in `infrastructure/api/grpc/service/`
7. Wire handler in `grpcModule.kt`

## Domain Error Mapping

Errors in `domain/common/error/DomainError.kt` are mapped to gRPC status codes in `infrastructure/api/grpc/error/`. The `mapOrThrowGrpc` extension handles automatic conversion with structured metadata headers (`x-error-code`, `x-identifier`, etc.).
