# iGambling Casino Engine

A private, open-source Kotlin microservice for iGambling operations, providing unified game aggregator integration, session management, betting operations, and event-driven architecture.

> **IMPORTANT:** This is a **private service** designed to run behind your own infrastructure. It requires custom adapters for wallet, player, and other integrations. You must implement a public-facing decorator/API layer for client access.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Getting Started](#getting-started)
4. [Use Cases](#use-cases)
5. [gRPC API Documentation](#grpc-api-documentation)
6. [Supported Aggregators](#supported-aggregators)
7. [Integrating a New Aggregator](#integrating-a-new-aggregator)
8. [Custom Adapters (Required)](#custom-adapters-required)
9. [Event System](#event-system)
10. [How the Service Works](#how-the-service-works)
11. [Configuration](#configuration)
12. [Error Handling](#error-handling)

---

## Architecture Overview

The service follows **Hexagonal Architecture** (Ports & Adapters) with **CQRS** and **Domain-Driven Design**:

```
┌─────────────────────────────────────────────────────────────────────┐
│                       API Layer (gRPC + REST)                       │
│  ┌────────────────────────┐  ┌──────────────────────────────────┐  │
│  │  gRPC Services (5)     │  │  REST Webhooks (Aggregators)     │  │
│  └────────────────────────┘  └──────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                       Application Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │  Use Cases  │  │  CQRS Bus   │  │   Events    │  │  Handlers │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│                         Domain Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │  Entities   │  │Value Objects│  │  Services   │  │   Errors  │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│                      Infrastructure Layer                           │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────────┐   │
│  │   Aggregators  │  │  Persistence │  │  Adapters             │   │
│  │  (Pragmatic,   │  │  (Exposed    │  │  - WalletAdapter      │   │
│  │   OneGameHub,  │  │   ORM)       │  │  - PlayerLimitAdapter │   │
│  │   Pateplay)    │  │              │  │  - S3FileAdapter      │   │
│  └────────────────┘  └──────────────┘  └───────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Source Structure

```
src/main/kotlin/
├── api/
│   ├── grpc/           # gRPC service implementations, mappers, interceptors
│   └── rest/           # Aggregator webhook REST endpoints
├── application/
│   ├── cqrs/           # Commands, Queries, Bus (organized by domain)
│   ├── event/          # Domain events (SessionOpen, Spin, RoundEnd)
│   ├── port/           # Port interfaces: storage/, external/, factory/
│   └── usecase/        # Orchestrators: OpenSession, ProcessSpin, FinishRound, SyncAggregator
├── domain/
│   ├── exception/      # DomainException hierarchy (notfound, badrequest, conflict, forbidden)
│   ├── model/          # Aggregates: Game, Session, Round, Spin, Provider, Collection, Aggregator
│   ├── service/        # Factories: SessionFactory, RoundFactory, SpinFactory, SpinBalanceCalculator
│   ├── util/           # Mutable traits: Activatable, Imageable, Orderable
│   └── vo/             # Value objects: Identity, Currency, Locale, Amount, PlayerId, SessionToken
└── infrastructure/
    ├── aggregator/     # Aggregator adapters (OneGameHub, Pragmatic, Pateplay)
    ├── handler/        # CQRS handler implementations
    ├── koin/           # DI modules (8 ordered modules)
    ├── persistence/    # Exposed ORM: tables, entities, mappers, repositories
    ├── rabbitmq/       # Event publisher + consumer + event mappers
    ├── redis/          # PlayerLimitRedis
    ├── s3/             # S3FileAdapter
    └── wallet/         # WalletAdapter (gRPC client to wallet-service)
```

---

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin (JVM 21) | 2.0.21 |
| Framework | Ktor Server (CIO) | 3.0.3 |
| Database | Exposed ORM + PostgreSQL | 0.57.0 |
| DI | Koin | 4.0.3 |
| gRPC | io.grpc + Protobuf | 1.68.2 / 4.29.2 |
| Messaging | RabbitMQ (ktor-server-rabbitmq) | 1.3.6 |
| Caching | Redis (Lettuce) | 6.5.3 |
| Storage | AWS S3 SDK for Kotlin | 1.3.99 |
| Serialization | kotlinx.serialization | — |
| Connection Pool | HikariCP | 5.1.0 |
| Build | Gradle (Kotlin DSL) | — |

---

## Getting Started

### Prerequisites

- JDK 21
- Docker & Docker Compose

### Local Development

```bash
# 1. Start infrastructure
docker-compose up -d postgres rabbitmq redis minio minio-init

# 2. Configure environment (defaults point to localhost)
cp .env.example .env

# 3. Run the application
./gradlew run                  # HTTP on :8080, gRPC on :5050
```

### Full Stack (Docker)

```bash
./gradlew build                # Creates build/distributions/casino-engine-*.tar
docker-compose up -d           # Starts infra + app + sync job
```

### Build Commands

```bash
./gradlew build                # Build (also runs installDist)
./gradlew test                 # Run all tests
./gradlew run                  # Run application (HTTP :8080, gRPC :5050)
./gradlew runSync              # Run aggregator sync CLI locally
./gradlew generateProto        # Generate gRPC stubs from proto files
./gradlew grpcClientJar        # Build gRPC client JAR for consumers
```

### Two Application Entrypoints

| Entrypoint | Command | Purpose |
|------------|---------|---------|
| Main Server | `/app/bin/casino-engine` | HTTP + gRPC + RabbitMQ consumers |
| Sync Job | `/app/bin/sync-aggregators` | One-shot game sync from all active aggregators |

---

## Use Cases

### Session Management

| Use Case | Description |
|----------|-------------|
| `OpenSessionUsecase` | Opens a new game session — creates adapter, gets launch URL from aggregator, saves session, publishes event |

### Spin (Betting) Operations

All spin operations are processed through `ProcessSpinUsecase`, which handles the full lifecycle:

| Spin Type | Flow |
|-----------|------|
| **PLACE** | Check player limits → calculate balance (real-first deduction) → withdraw from wallet → save spin → publish event |
| **SETTLE** | Calculate win amounts → deposit to wallet (same pool as original bet) → save spin → publish event |
| **ROLLBACK** | Refund to original pools → save rollback spin → publish event |

**Freespin rounds** skip balance checks entirely — wallet operations are bypassed.

### Round Management

| Use Case | Description |
|----------|-------------|
| `FinishRoundUsecase` | Marks round as finished, publishes `RoundEndEvent` |

### Game Sync

| Use Case | Description |
|----------|-------------|
| `SyncAggregatorUsecase` | Syncs games from an aggregator — fetches game list, creates/updates providers, games, and variants |

### CQRS Handlers

Beyond use cases, the service uses CQRS handlers for catalog management:

**Game Management:**
- `SaveGameCommand` — Update game configuration (active, bonus settings, tags)
- `FindGameQuery` / `FindAllGameQuery` / `BatchGameQuery` — Query games with filtering & pagination
- `UpdateGameImageCommand` — Upload/update game image by key
- `PlayGameCommand` — Open real-money game session, returns launch URL
- `OpenDemoQuery` — Get demo game launch URL
- `GameFavouriteCommand` — Add/remove game from player favorites

**Collection Management:**
- `SaveCollectionCommand` — Create/update collection with localized names
- `FindCollectionQuery` / `FindAllCollectionQuery` — Query collections with game/provider counts
- `UpdateCollectionGamesCommand` — Add/remove games from a collection
- `UpdateCollectionImageCommand` — Upload/update collection image

**Provider Management:**
- `SaveProviderCommand` — Create/update provider
- `FindProviderQuery` / `FindAllProviderQuery` — Query providers with active/inactive game counts
- `UpdateProviderImageCommand` — Upload/update provider image

**Aggregator Management:**
- `SaveAggregatorCommand` — Create/update aggregator with config
- `FindAggregatorQuery` / `FindAllAggregatorQuery` — Query aggregators, filter by integration type
- `SyncAllActiveAggregatorCommand` — Sync games from all active aggregators

**Freespin Management:**
- `GetFreespinPresetQuery` — Get freespin preset configuration from aggregator
- `CreateFreespinCommand` — Create a freespin bonus for a player
- `CancelFreespinCommand` — Cancel an active freespin

---

## gRPC API Documentation

Proto package: `game.v1` (Java: `com.nekgamebling.game.v1`)

### GameService

```protobuf
service GameService {
  rpc Save(SaveGameCommand) returns (Empty);
  rpc Find(FindGameQuery) returns (FindGameQuery.Result);
  rpc FindAll(FindAllGameQuery) returns (FindAllGameQuery.Result);
  rpc Batch(BatchGameQuery) returns (BatchGameQuery.Result);
  rpc UpdateImage(UpdateGameImageCommand) returns (Empty);
  rpc Play(PlayGameCommand) returns (PlayGameCommand.Result);
  rpc OpenDemo(OpenDemoQuery) returns (OpenDemoQuery.Result);
  rpc AddFavourite(GameFavouriteCommand) returns (Empty);
  rpc RemoveFavourite(GameFavouriteCommand) returns (Empty);
}

// Open a real-money game session
message PlayGameCommand {
  string identity = 1;                   // Game identifier
  string player_id = 2;                  // Your player ID
  string locale = 3;                     // Locale (e.g., "en", "de")
  PlatformDto platform = 4;             // DESKTOP, MOBILE, DOWNLOAD
  string currency = 5;                   // Currency code (e.g., "EUR")
  optional int64 max_spin_place_amount = 6;  // Max bet limit for player

  message Result {
    string launch_url = 1;               // URL to launch the game
  }
}

// Open a demo game
message OpenDemoQuery {
  string identity = 1;                   // Game identifier
  string currency = 2;                   // Currency code
  string locale = 3;                     // Locale
  PlatformDto platform = 4;             // Platform type
  string lobby_url = 5;                  // Return URL after game exit

  message Result {
    string launch_url = 1;               // Demo game URL
  }
}

// Reusable filter shape — also used by future game-listing RPCs
message GameFilter {
  string query = 1;                      // Search query
  optional bool active = 2;              // Filter by active status
  optional string provider_identity = 3;
  repeated string tags = 4;
  optional bool bonus_bet_enable = 5;
  optional bool bonus_wagering_enable = 6;
  optional bool free_spin_enable = 7;
  optional bool free_chip_enable = 8;
  optional bool jackpot_enable = 9;
  optional bool demo_enable = 10;
  optional bool bonus_buy_enable = 11;
}

// List games with filtering and pagination
message FindAllGameQuery {
  GameFilter filter = 1;
  int32 page_num = 2;
  int32 page_size = 3;

  message Result {
    repeated Item items = 1;             // Game items with provider
    repeated ProviderDto providers = 2;  // Available providers
    repeated AggregatorDto aggregators = 3;
    repeated CollectionDto collections = 4;
    int32 total_items = 5;
  }
}

// Upload game image
message UpdateGameImageCommand {
  string identity = 1;                   // Game identifier
  string key = 2;                        // Image key (e.g., "thumbnail", "banner")
  bytes file = 3;                        // Image binary data
  string extension = 4;                  // File extension (e.g., "png", "jpg")
}
```

### ProviderService

```protobuf
service ProviderService {
  rpc Save(ProviderDto) returns (Empty);
  rpc Find(FindProviderQuery) returns (FindProviderQuery.Result);
  rpc FindAll(FindAllProviderQuery) returns (FindAllProviderQuery.Result);
  rpc UpdateImage(UpdateProviderImageCommand) returns (Empty);
}

message FindProviderQuery {
  string identity = 1;
  message Result {
    ProviderDto item = 1;
    AggregatorDto aggregator = 2;
    int32 active_game_count = 3;         // Number of active games
    int32 deactivate_game_count = 4;     // Number of inactive games
  }
}

message FindAllProviderQuery {
  string query = 1;
  optional bool active = 2;
  optional string aggregator_identity = 3;  // Filter by aggregator
  int32 page_num = 4;
  int32 page_size = 5;
}
```

### CollectionService

```protobuf
service CollectionService {
  rpc Save(CollectionDto) returns (Empty);
  rpc Find(FindCollectionQuery) returns (FindCollectionQuery.Result);
  rpc FindAll(FindAllCollectionQuery) returns (FindAllCollectionQuery.Result);
  rpc UpdateGames(UpdateCollectionGamesCommand) returns (Empty);
  rpc UpdateImage(UpdateCollectionImageCommand) returns (Empty);
}

message UpdateCollectionGamesCommand {
  string identity = 1;                    // Collection identifier
  repeated string add_games = 2;          // Game identities to add
  repeated string remove_games = 3;       // Game identities to remove
}

// Collection supports localized names
message CollectionDto {
  string identity = 1;
  map<string, string> name = 2;           // {"en": "Popular", "de": "Beliebt"}
  map<string, string> images = 3;
  bool active = 4;
  int32 order = 5;
}
```

### AggregatorService

```protobuf
service AggregatorService {
  rpc Save(AggregatorDto) returns (Empty);
  rpc Find(FindAggregatorQuery) returns (AggregatorDto);
  rpc FindAll(FindAllAggregatorQuery) returns (FindAllAggregatorResult);
}

message AggregatorDto {
  string identity = 1;                    // Unique identifier
  string integration = 2;                // ONEGAMEHUB, PRAGMATIC, PATEPLAY
  google.protobuf.Struct config = 3;     // Aggregator-specific configuration
  bool active = 4;
}

message FindAllAggregatorQuery {
  string query = 1;
  optional bool active = 2;
  optional string integration = 3;       // Filter by integration type
  int32 page_num = 4;
  int32 page_size = 5;
}
```

### FreespinService

```protobuf
service FreespinService {
  rpc GetPreset(GetFreespinPresetQuery) returns (GetFreespinPresetQuery.Result);
  rpc Create(CreateFreespinCommand) returns (Empty);
  rpc Cancel(CancelFreespinCommand) returns (Empty);
}

message CreateFreespinCommand {
  string game_identity = 1;
  string player_id = 2;
  string reference_id = 3;                // Your reference ID
  string currency = 4;
  string start_at = 5;                    // ISO datetime
  string end_at = 6;                      // ISO datetime
  google.protobuf.Struct preset_values = 7;  // Aggregator-specific preset config
}

message GetFreespinPresetQuery {
  string game_identity = 1;
  message Result {
    google.protobuf.Struct preset = 1;    // Available preset options (JSON)
  }
}
```

### DTO Reference

```protobuf
// Game DTO — full game representation
message GameDto {
  string identity = 1;
  string name = 2;
  string provider_identity = 3;
  repeated string collection_identities = 4;
  bool bonus_bet_enable = 5;
  bool bonus_wagering_enable = 6;
  repeated string tags = 7;
  bool active = 8;
  map<string, string> images = 9;
  int32 order = 10;
  string symbol = 11;                     // Aggregator game symbol
  string integration = 12;               // Aggregator type
  bool free_spin_enable = 14;
  bool free_chip_enable = 15;
  bool jackpot_enable = 16;
  bool demo_enable = 17;
  bool bonus_buy_enable = 18;
  repeated string locales = 19;
  repeated PlatformDto platforms = 20;    // DESKTOP, MOBILE, DOWNLOAD
  int32 play_lines = 21;
}

// Provider DTO
message ProviderDto {
  string identity = 1;
  string name = 2;
  map<string, string> images = 3;
  int32 order = 4;
  bool active = 5;
  string aggregator_identity = 6;
}

// Platform enum
enum PlatformDto {
  PLATFORM_UNSPECIFIED = 0;
  PLATFORM_DESKTOP = 1;
  PLATFORM_MOBILE = 2;
  PLATFORM_DOWNLOAD = 3;
}
```

---

## Supported Aggregators

### 1. Pragmatic Play

**Type:** `PRAGMATIC`

**Configuration:**

| Key | Description | Required |
|-----|-------------|----------|
| `secretKey` | API secret key provided by Pragmatic | Yes |
| `secureLogin` | Secure login identifier | Yes |
| `gatewayUrl` | Pragmatic API gateway URL | Yes |

**Example:**
```json
{
  "secretKey": "your-secret-key",
  "secureLogin": "your-secure-login",
  "gatewayUrl": "https://api.pragmaticplay.net"
}
```

**Authentication:** MD5 hash (sorted params + secret key)

**Callback Endpoints** (GET at `/pragmatic/*.html`):
- `/authenticate.html` — Validates session token
- `/balance.html` — Returns player balance
- `/bet.html` — Processes bet placement
- `/result.html` — Processes spin result/win
- `/bonusWin.html` — Bonus win notification
- `/jackpotWin.html` — Jackpot win notification
- `/refund.html` — Refunds a transaction
- `/endRound.html` — Closes the round
- `/adjustment.html` — Manual balance adjustment

**Amount format:** Decimal strings converted to/from minor units (×100)

---

### 2. OneGameHub

**Type:** `ONEGAMEHUB`

**Configuration:**

| Key | Description | Required |
|-----|-------------|----------|
| `salt` | Encryption salt | Yes |
| `secret` | API secret | Yes |
| `partner` | Partner identifier | Yes |
| `gateway` | OneGameHub API gateway URL | Yes |

**Example:**
```json
{
  "salt": "your-salt",
  "secret": "your-secret",
  "partner": "your-partner-id",
  "gateway": "https://api.onegamehub.com"
}
```

**Callback Endpoint** (POST at `/onegamehub`):
- Actions via query parameter: `balance`, `bet`, `win`
- Session token via `extra` query parameter

---

### 3. Pateplay

**Type:** `PATEPLAY`

**Configuration:**

| Key | Description | Required |
|-----|-------------|----------|
| `gatewayUrl` | Pateplay API gateway URL | Yes |
| `siteCode` | Site identifier | Yes |
| `gatewayApiKey` | API key for gateway | Yes |
| `gatewayApiSecret` | API secret for gateway | Yes |
| `gameLaunchUrl` | Base URL for game launch | Yes |
| `gameDemoLaunchUrl` | Base URL for demo games | Yes |
| `walletApiKey` | Wallet API key | Yes |
| `walletApiSecret` | Wallet API secret | Yes |

**Example:**
```json
{
  "gatewayUrl": "https://api.pateplay.com",
  "siteCode": "your-site-code",
  "gatewayApiKey": "your-api-key",
  "gatewayApiSecret": "your-api-secret",
  "gameLaunchUrl": "https://games.pateplay.com/launch",
  "gameDemoLaunchUrl": "https://games.pateplay.com/demo",
  "walletApiKey": "your-wallet-key",
  "walletApiSecret": "your-wallet-secret"
}
```

**Authentication:** HMAC-SHA256 for freespin API

**Notes:** Static game catalog (no game discovery API), launch URLs constructed locally. Wallet callback handler not yet implemented.

---

## Integrating a New Aggregator

### Step 1: Create Configuration Model

Create `infrastructure/aggregator/youraggregator/model/YourConfig.kt`:

```kotlin
internal class YourConfig(config: Map<String, String>) {
    val apiKey = config["apiKey"] ?: ""
    val secretKey = config["secretKey"] ?: ""
    val gatewayUrl = config["gatewayUrl"] ?: ""
}
```

### Step 2: Implement Game Adapter (`IGamePort`)

```kotlin
class YourGameAdapter(
    private val aggregator: Aggregator
) : IGamePort {

    override suspend fun getAggregatorGames(): List<AggregatorGame> {
        // Fetch games from aggregator API
    }

    override suspend fun getDemoUrl(
        gameSymbol: String, locale: Locale, platform: Platform,
        currency: Currency, lobbyUrl: String
    ): String {
        // Build demo launch URL
    }

    override suspend fun getLunchUrl(session: Session, lobbyUrl: String): String {
        // Build real-money launch URL
    }
}
```

### Step 3: Implement Freespin Adapter (`IFreespinPort`)

```kotlin
class YourFreespinAdapter(
    private val aggregator: Aggregator
) : IFreespinPort {

    override suspend fun getPreset(gameSymbol: String): Map<String, Any> { ... }
    override suspend fun create(...) { ... }
    override suspend fun cancel(referenceId: String) { ... }
}
```

### Step 4: Register in `AggregatorFabricImpl`

Update `AggregatorFabricImpl` to route your integration string to the new adapters:

```kotlin
"YOUR_AGGREGATOR" -> YourGameAdapter(aggregator)
```

### Step 5: Create Webhook Handler (if needed)

```kotlin
fun Route.yourAggregatorRoutes(handler: YourHandler) {
    route("/youraggregator") {
        post("/balance") { ... }
        post("/bet") { ... }
        post("/win") { ... }
        post("/refund") { ... }
    }
}
```

### Step 6: Register in Koin Module

Create your Koin module and include it in `AggregatorModule`. Register webhook routes in `Main.kt`.

---

## Custom Adapters (Required)

The service defines port interfaces that you must implement for production use.

### IWalletPort

Interface: `application/port/external/IWalletPort.kt`

```kotlin
interface IWalletPort {
    suspend fun findBalance(playerId: PlayerId, currency: Currency): PlayerBalance

    suspend fun withdraw(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount
    ): PlayerBalance

    suspend fun deposit(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount
    ): PlayerBalance
}
```

**Included implementation:** `WalletAdapter` — gRPC client to the companion `wallet-service`.

### IPlayerLimitPort

Interface: `application/port/external/IPlayerLimitPort.kt`

```kotlin
interface IPlayerLimitPort {
    suspend fun getMaxPlaceAmount(playerId: PlayerId): Amount?
    suspend fun saveMaxPlaceAmount(playerId: PlayerId, amount: Amount)
}
```

**Included implementation:** `PlayerLimitRedis` — Redis-backed with TTL.

### FileAdapter

Interface: `application/port/external/FileAdapter.kt`

```kotlin
data class MediaFile(
    val ext: String,
    val bytes: ByteArray
)

interface FileAdapter {
    suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String>
    suspend fun delete(path: String): Result<Boolean>
}
```

**Included implementation:** `S3FileAdapter` — S3/MinIO-compatible storage.

### IEventPort

Interface: `application/port/external/IEventPort.kt`

```kotlin
interface IEventPort {
    suspend fun publish(event: ApplicationEvent)
}
```

**Included implementation:** `RabbitMqEventPublisher` — publishes domain events to RabbitMQ exchange.

### ICurrencyPort

Interface: `application/port/external/ICurrencyPort.kt`

```kotlin
interface ICurrencyPort {
    suspend fun convertToUnits(amount: Double, currency: Currency): Long
    suspend fun convertFromUnits(amount: Long, currency: Currency): Double
}
```

**Included implementation:** `CurrencyAdapter` — minor unit conversion (×100).

### Registering Custom Adapters

Wire your implementations in the appropriate Koin module (`externalModule`):

```kotlin
val externalModule = module {
    single<IWalletPort> { YourWalletAdapter(/* dependencies */) }
    single<IPlayerLimitPort> { YourPlayerLimitAdapter(/* dependencies */) }
    single<IEventPort> { YourEventPublisher(/* dependencies */) }
}
```

---

## Event System

The service publishes domain events via RabbitMQ. Subscribe to these events for analytics, notifications, or downstream processing.

### Available Events

| Event | Routing Key | Description |
|-------|-------------|-------------|
| `SessionOpenEvent` | `session.opened` | New game session created |
| `SpinEvent` (PLACE) | `spin.placed` | Bet was placed |
| `SpinEvent` (SETTLE) | `spin.settled` | Spin result settled (win/loss) |
| `SpinEvent` (ROLLBACK) | `spin.rollback` | Bet was refunded |
| `RoundEndEvent` | `round.finished` | Round was closed |
| `GameFavouriteAdded` | `game.favourite.added` | Game added to favorites |
| `GameFavouriteRemoved` | `game.favourite.removed` | Game removed from favorites |
| `GameWon` | `game.won` | Win recorded |

### Event Payloads

**SessionOpenEvent:**
```kotlin
data class SessionOpenEvent(val session: Session) : ApplicationEvent
// Session contains: token, playerId, currency, locale, platform, game reference
```

**SpinEvent:**
```kotlin
data class SpinEvent(val spin: Spin) : ApplicationEvent
// Spin contains: type (PLACE/SETTLE/ROLLBACK), amounts (real/bonus), transactionId, round reference
```

**RoundEndEvent:**
```kotlin
data class RoundEndEvent(val round: Round) : ApplicationEvent
// Round contains: id, session reference, finished flag
```

### Consumer

`PlaceSpinEventConsumer` subscribes to `spin.placed` events and enforces player betting limits by updating max place amounts via `IPlayerLimitPort` (Redis).

---

## How the Service Works

### Session Flow

```
┌──────────┐    1. Play(game, player)    ┌─────────────┐    2. Get Launch URL    ┌────────────┐
│  Client  │ ────────────────────────────►│ Casino      │ ───────────────────────►│ Aggregator │
│          │ ◄────────────────────────────│ Engine      │ ◄─────────────────────── │            │
└──────────┘    4. Launch URL            └─────────────┘    3. Launch URL        └────────────┘
     │                                         │
     │         5. Launch Game                  │
     └─────────────────────────────────────────┼────────────────────────────────────────►
                                               │
                                               │  6. Save Session + Publish Event
                                               ▼
                                    ┌────────────────────┐
                                    │ Database + RabbitMQ │
                                    └────────────────────┘
```

### Betting Flow

```
┌────────────┐   1. Bet Callback      ┌─────────────┐   2. Find Session
│ Aggregator │ ──────────────────────►│ Webhook     │ ──────────────────►
│            │ ◄──────────────────────│ Handler     │
└────────────┘   6. Balance Response  └─────────────┘
                                            │
                                            │ 3. ProcessSpin
                                            ▼
                                      ┌──────────────┐    4. Withdraw/Deposit
                                      │ProcessSpin   │ ──────────────────────►
                                      │Usecase       │          ┌─────────────┐
                                      └──────────────┘          │ IWalletPort │
                                            │                   └─────────────┘
                                            │ 5. Publish Event
                                            ▼
                                      ┌─────────────┐
                                      │  RabbitMQ   │
                                      └─────────────┘
```

### Round Lifecycle

1. **First Bet** → Round created with `extId` from aggregator
2. **Additional Bets** → Same round reused (matched by `extId`)
3. **Settle** → Win/loss recorded, funds deposited to wallet
4. **End Round** → Round marked as finished
5. **Rollback** → Previous spin reversed, funds refunded to original pools

### Balance Calculation (`SpinBalanceCalculator`)

| Spin Type | Real Balance | Bonus Balance |
|-----------|-------------|---------------|
| **PLACE** | Deducts real amount (real-first when bonusBet) | Deducts bonus amount |
| **SETTLE** | Deposits to same pool as original bet | Deposits to same pool as original bet |
| **ROLLBACK** | Refunds to original pool | Refunds to original pool |

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `HTTP_PORT` | HTTP server port | `8080` |
| `GRPC_PORT` | gRPC server port | `5050` |
| `DATABASE_URL` | JDBC PostgreSQL URL | `jdbc:postgresql://localhost:5432/game_db` |
| `DATABASE_USER` | Database username | — |
| `DATABASE_PASSWORD` | Database password | — |
| `WALLET_GRPC_HOST` | Wallet service gRPC host | `localhost` |
| `WALLET_GRPC_PORT` | Wallet service gRPC port | `5555` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `S3_ENDPOINT` | S3-compatible storage endpoint | — |
| `S3_REGION` | S3 region | — |
| `S3_ACCESS_KEY` | S3 access key | — |
| `S3_SECRET_KEY` | S3 secret key | — |
| `S3_BUCKET` | S3 bucket name | — |
| `RABBITMQ_URL` | RabbitMQ AMQP URL | `amqp://guest:guest@localhost:5672` |
| `RABBITMQ_EXCHANGE` | RabbitMQ exchange name | `casino-engine` |

### Docker Infrastructure

| Service | Port(s) | Purpose |
|---------|---------|---------|
| PostgreSQL 16 | 5432 | Database |
| RabbitMQ 3 | 5672, 15672 | Message broker + management UI |
| Redis 7 | 6379 | Player limits cache |
| MinIO | 9000, 9001 | S3-compatible file storage + console |

### gRPC Client Publishing

The service publishes a gRPC client JAR for downstream consumers:

```bash
# Build and publish
./gradlew grpcClientJar grpcClientSourcesJar \
  -PgrpcClientVersion=1.0.0

# Maven coordinates
com.nekgamebling:game-grpc-client:1.0.0
```

CI/CD via GitHub Actions (`publish-grpc-client.yml`) publishes to GitHub Packages on tag push (`v*`).

---

## Error Handling

The service uses typed domain exceptions mapped to gRPC status codes. The `x-exception-name` metadata header carries the exception class name for downstream error identification.

### Exception Hierarchy

| Category | gRPC Status | Exceptions |
|----------|-------------|------------|
| **NotFoundException** | `NOT_FOUND` | `SessionNotFoundException`, `RoundNotFoundException`, `GameNotFoundException`, `CollectionNotFoundException` |
| **BadRequestException** | `INVALID_ARGUMENT` | `BlankSessionTokenException`, `BlankLocaleException`, `BlankCurrencyException`, `BlankPlayerIdException`, `InvalidAmountException`, `EmptyIdentityException`, `InvalidIdentityFormatException`, `SpinReferenceRequiredException`, `UnsupportedLocaleException`, `UnsupportedPlatformException` |
| **ConflictException** | `ALREADY_EXISTS` | `RoundAlreadyFinishedException`, `GameNotActiveException`, `ProviderNotActiveException`, `AggregatorNotActiveException`, `FreespinNotSupportedException` |
| **ForbiddenException** | `PERMISSION_DENIED` | `InsufficientBalanceException`, `MaxPlaceSpinException` |
| **SystemException** | `INTERNAL` | Internal/unexpected errors |

### Helper Functions

```kotlin
// Throws categorized DomainException if value is null
domainRequireNotNull(value) { GameNotFoundException() }

// Throws categorized DomainException if condition is false
domainRequire(round.isActive) { RoundAlreadyFinishedException() }
```

---

## Public API Decorator

**This service is private and should NOT be exposed directly to clients.**

You must implement a public-facing decorator that:

1. **Authenticates** requests (JWT, API keys, etc.)
2. **Authorizes** player access
3. **Rate limits** requests
4. **Logs** and monitors traffic
5. **Transforms** responses for your client format

```
┌──────────┐      ┌────────────────┐      ┌─────────────────┐
│  Client  │ ───► │  Your Public   │ ───► │  Casino Engine  │
│          │ ◄─── │  API Gateway   │ ◄─── │  (gRPC :5050)   │
└──────────┘      └────────────────┘      └─────────────────┘
                         │
                         ▼
                  ┌─────────────┐
                  │ Auth / Rate │
                  │ Limiting    │
                  └─────────────┘
```

---
