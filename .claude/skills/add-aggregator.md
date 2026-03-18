---
description: Add a new game aggregator integration (e.g. "add aggregator Softswiss", "new aggregator provider", "integrate new game provider")
---

# Add New Aggregator

Create a full aggregator integration under `src/main/kotlin/infrastructure/aggregator/<name>/`.

## Required Input

Ask the user for:
1. **Aggregator name** (e.g. `Softswiss`, `Evoplay`) — used for class/package naming
2. **API base URL pattern** and **authentication method** (key, secret, hash, etc.)
3. **API docs or endpoint list** — what endpoints exist for: list games, launch URL, freespins, webhooks

## Directory Structure

Create this exact structure (replace `<Name>` with PascalCase, `<name>` with lowercase):

```
infrastructure/aggregator/<name>/
├── <Name>Config.kt                  # Config from Map<String, Any>
├── <Name>AdapterFactory.kt          # Creates adapters
├── adapter/
│   ├── <Name>GameAdapter.kt         # Implements IGamePort
│   └── <Name>FreespinAdapter.kt     # Implements IFreespinPort
├── client/
│   ├── <Name>HttpClient.kt          # Ktor HTTP client
│   └── dto/
│       ├── ResponseDto.kt           # Generic API response wrapper
│       ├── GameDto.kt               # Game list response
│       ├── GameUrlDto.kt            # Launch URL response
│       ├── CreateFreespinDto.kt     # Freespin create payload
│       └── CancelFreespinDto.kt     # Freespin cancel payload
├── tool/                            # Custom serializers if needed
└── webhook/
    ├── <Name>Webhook.kt             # Ktor route handler for callbacks
    └── dto/
        ├── <Name>Response.kt        # Webhook response sealed class
        └── <Name>BetDto.kt          # Internal bet/win DTO
```

## Implementation Pattern

### 1. Config (`<Name>Config.kt`)

```kotlin
package infrastructure.aggregator.<name>

class <Name>Config(config: Map<String, Any>) {
    val apiKey = config["apiKey"]?.toString() ?: ""
    val secret = config["secret"]?.toString() ?: ""
    val gateway = config["gateway"]?.toString() ?: ""
    // ... aggregator-specific fields
}
```

### 2. HTTP Client (`client/<Name>HttpClient.kt`)

```kotlin
package infrastructure.aggregator.<name>.client

class <Name>HttpClient(private val config: <Name>Config) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    suspend fun listGames(): ResponseDto<List<GameDto>> { ... }
    suspend fun getLaunchUrl(...): ResponseDto<GameUrlDto> { ... }
    suspend fun createFreespin(payload: CreateFreespinDto): ResponseDto<String> { ... }
    suspend fun cancelFreespin(payload: CancelFreespinDto): ResponseDto<String> { ... }
}
```

### 3. DTOs (`client/dto/`)

- All DTOs are `@Serializable` data classes
- Use `@SerialName` for API field mapping
- `ResponseDto<T>` wraps all API responses with `status: Int` and `response: T?` and computed `val success: Boolean`
- Use custom serializers in `tool/` package if needed (e.g. `LocalDateTimeAsStringSerializer`)

### 4. Game Adapter (`adapter/<Name>GameAdapter.kt`)

Implements `application.port.external.IGamePort`. Constructor takes only `<Name>Config`. Creates `<Name>HttpClient` internally.

- `getAggregatorGames()` — call client to list games, map **all** results to `List<IGamePort.AggregatorGame>`
- `getDemoUrl(gameSymbol, locale, platform, currency, lobbyUrl)` — call client for demo URL
- `getLunchUrl(session, lobbyUrl)` — call client for real-play URL using session fields (playerId, sessionToken, locale, platform, currency)

Validate responses with `check(response.success)` and `requireNotNull`.

### 5. Freespin Adapter (`adapter/<Name>FreespinAdapter.kt`)

Implements `application.port.external.IFreespinPort`. Constructor takes only `<Name>Config`. Creates `<Name>HttpClient` internally.

- `getPreset(gameSymbol)` — return aggregator-specific preset as `Map<String, Any>`
- `create(presetValue, referenceId, playerId, gameSymbol, currency, startAt, endAt)` — build DTO from preset values, call client
- `cancel(referenceId)` — call client to cancel

### 6. Adapter Factory (`<Name>AdapterFactory.kt`)

```kotlin
package infrastructure.aggregator.<name>

class <Name>AdapterFactory {
    fun createGameAdapter(config: Map<String, Any>) = <Name>GameAdapter(createConfig(config))
    fun createFreespinAdapter(config: Map<String, Any>) = <Name>FreespinAdapter(createConfig(config))
    private fun createConfig(config: Map<String, Any>) = <Name>Config(config)
}
```

### 7. Webhook (`webhook/<Name>Webhook.kt`)

Constructor takes `bus: Bus` for CQRS command dispatching. Parses request parameters and dispatches domain commands.

```kotlin
class <Name>Webhook(private val bus: Bus) {
    // Parameter extension properties for parsing request params
    private val Parameters.amount get() = this["amount"]!!.toLong()
    private val Parameters.gameSymbol get() = this["game_id"]!!
    private val Parameters.transactionId get() = this["transaction_id"]!!
    private val Parameters.roundId get() = this["round_id"]!!
    private val Parameters.freespinId get() = this["freerounds_id"]
    private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

    fun Route.route() = post("/<name>") {
        val action = call.request.queryParameters["action"]!!
        val sessionToken = call.request.queryParameters["extra"]!!  // session token

        val response = when (action) {
            "balance" -> balance(sessionToken)
            "bet" -> bet(sessionToken, call.request.queryParameters)
            "win" -> win(sessionToken, call.request.queryParameters)
            else -> <Name>Response.Error.UNEXPECTED_ERROR
        }
        call.respond(response)
    }

    private suspend fun balance(sessionToken: String): <Name>Response {
        val result = bus.execute(FindSessionBalanceQuery(SessionToken(sessionToken)))
        return <Name>Response.Success(balance = result.balance, currency = result.currency)
    }

    private suspend fun bet(sessionToken: String, parameters: Parameters): <Name>Response {
        val result = bus.execute(PlaceSpinSessionCommand(
            sessionToken = SessionToken(sessionToken),
            amount = Amount(parameters.amount),
            gameSymbol = parameters.gameSymbol,
            transactionId = parameters.transactionId,
            roundId = parameters.roundId,
            freespinId = parameters.freespinId
        ))
        return <Name>Response.Success(balance = result.balance, currency = result.currency)
    }

    private suspend fun win(sessionToken: String, parameters: Parameters): <Name>Response {
        val result = bus.execute(SettleSpinSessionCommand(
            sessionToken = SessionToken(sessionToken),
            amount = Amount(parameters.amount),
            gameSymbol = parameters.gameSymbol,
            transactionId = parameters.transactionId,
            roundId = parameters.roundId,
            freespinId = parameters.freespinId
        ))
        if (parameters.isRoundEnd) {
            bus.execute(EndRoundSessionCommand(
                sessionToken = SessionToken(sessionToken),
                roundId = parameters.roundId
            ))
        }
        return <Name>Response.Success(balance = result.balance, currency = result.currency)
    }
}
```

**Error mapping** — Wrap each action in try-catch and map domain exceptions:
- `SessionNotFoundException` → `SESSION_TIMEOUT`
- `InsufficientBalanceException` → `INSUFFICIENT_FUNDS`
- `MaxPlaceSpinException` → `EXCEED_WAGER_LIMIT`
- Other exceptions → `UNEXPECTED_ERROR`

### 8. Webhook Response (`webhook/dto/<Name>Response.kt`)

Sealed class with `Success(balance: Long, currency: String)` and `Error(code, display, action, message, description)` subclasses. Error companion object with standard error constants: `UNEXPECTED_ERROR`, `SESSION_TIMEOUT`, `INSUFFICIENT_FUNDS`, `EXCEED_WAGER_LIMIT`, `AUTH_FAILED`, `UNAUTHORIZED`, `UNSUPPORTED_CURRENCY`, `BONUS_BET_MAX_RESTRICTION`.

## Wiring (4 files to update)

### 1. Update `AggregatorFabricImpl.kt`

Add the new factory as a constructor parameter and add `"<NAME>"` branch to both `when` blocks:

```kotlin
class AggregatorFabricImpl(
    private val oneGamehubAdapterFactory: OneGamehubAdapterFactory,
    private val <name>AdapterFactory: <Name>AdapterFactory,  // ADD
) : IAggregatoryFactory {
    override fun createGameAdapter(aggregator: Aggregator): IGamePort {
        return when (aggregator.integration) {
            "ONEGAMEHUB" -> oneGamehubAdapterFactory.createGameAdapter(aggregator.config)
            "<NAME>" -> <name>AdapterFactory.createGameAdapter(aggregator.config)  // ADD
            else -> error("Unsupported aggregator integration: ${aggregator.integration}")
        }
    }
    // same for createFreespinAdapter
}
```

### 2. Update `infrastructure/koin/ExternalModule.kt`

Register the new adapter factory and update `AggregatorFabricImpl` constructor:

```kotlin
single { <Name>AdapterFactory() }  // ADD
single<IAggregatoryFactory> {
    AggregatorFabricImpl(
        oneGamehubAdapterFactory = get(),
        <name>AdapterFactory = get(),  // ADD
    )
}
```

### 3. Update `infrastructure/koin/AggregatorModule.kt`

Register the webhook with `Bus` dependency:

```kotlin
single { <Name>Webhook(bus = get()) }  // ADD
```

### 4. Update `Main.kt` — `configureRouting()`

Retrieve the webhook from Koin and register its routes:

```kotlin
private fun Application.configureRouting() {
    val oneGameHubWebhook = get<OneGameHubWebhook>()
    val <name>Webhook = get<<Name>Webhook>()  // ADD

    routing {
        with(oneGameHubWebhook) { route() }
        with(<name>Webhook) { route() }  // ADD
    }
}
```

## Checklist

- [ ] Config class parses all required fields from `Map<String, Any>`
- [ ] HTTP client covers all API endpoints with proper auth
- [ ] All DTOs are `@Serializable` with correct `@SerialName` mappings
- [ ] `GameAdapter` implements all 3 `IGamePort` methods (`getAggregatorGames`, `getDemoUrl`, `getLunchUrl`)
- [ ] `FreespinAdapter` implements all 3 `IFreespinPort` methods
- [ ] `AdapterFactory` creates both adapters from config map
- [ ] Webhook takes `Bus` in constructor and dispatches CQRS commands
- [ ] Webhook maps domain exceptions to aggregator error codes
- [ ] Webhook response sealed class has Success + Error with standard error constants
- [ ] `AggregatorFabricImpl` updated with new factory + `when` branches
- [ ] `ExternalModule` registers new factory and updates `AggregatorFabricImpl` binding
- [ ] `AggregatorModule` registers new webhook with `bus = get()`
- [ ] `Main.kt configureRouting()` retrieves and registers new webhook routes
- [ ] `./gradlew compileKotlin` passes
