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
docker-compose up -d postgres rabbitmq redis

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

Hexagonal Architecture + DDD + CQRS. Kotlin 2.0.21, JDK 21, Ktor 3.0.3 (CIO), Exposed ORM, Koin DI, gRPC + protobuf, RabbitMQ, Redis (Lettuce). Dependency versions managed in `gradle/libs.versions.toml`.

Four layers: `api/` (gRPC services + REST webhooks) → `application/` (commands/queries, use cases, application ports, events, projections) → `domain/` (models, value objects, factories, repositories, exceptions, events) → `infrastructure/` (adapters, persistence, aggregators, messaging).

### Package layout

```
domain/
├── model/         # Aggregates, entities — @Serializable; published on the event bus AS-IS (no snapshot twins)
├── event/         # Events ARE a domain concern: AppEvent<T> + Meta marker, plus SpinEvent/CasinoRoundEvent/CasinoSessionEvent (each wraps a domain model directly)
├── vo/            # Value objects (@JvmInline value class with init validation)
├── service/       # Domain services (SpinBalanceCalculator, factories)
├── repository/    # Repository INTERFACES (DDD-pure: contracts live with the model)
├── exception/     # Sealed DomainException hierarchy
└── util/          # Trait interfaces (Activatable, Imageable, Orderable) + AnyMapSerializer (config Map<String,Any>)

application/
├── Bus.kt                 # CQRS bus contract
├── IHandler.kt            # CqrsHandler marker, ICommand, IQuery, ICommandHandler, IQueryHandler
├── HandlerRegistry.kt     # Reflective registry — auto-discovery
├── command/<feature>/     # Write-side: command DTOs (no handlers — those live in infra)
├── query/<feature>/       # Read-side: query DTOs + their View result types side-by-side
├── usecase/               # Application services / use cases (orchestrators)
├── port/external/         # Driven ports for external systems (IWalletPort, IPlayerLimitPort, IEventPublisherPort, ...)
└── port/factory/          # Driven ports for adapter factories (AggregatorAdapterProvider, IAggregatorFactory)

infrastructure/
├── handler/<feature>/     # Command/query handler IMPLEMENTATIONS (touch DB / repos / external)
├── persistence/           # Exposed repositories implementing domain.repository contracts
├── aggregator/<vendor>/   # Aggregator integration adapters
├── rabbitmq/              # AppEventBus.kt (RabbitAppEventPublisher impl of IEventPublisherPort, NoOpAppEventPublisher, AppEventConsumer base, appJson, EVENT_EXCHANGE) + consumers
├── redis/                 # Player limit cache
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
6. `configureWebhook()` — registers all inbound aggregator webhook routes under `/api/webhook` (OneGameHub, Pragmatic, TONGame) on the shared Ktor HTTP server (defined in `api/webhook/WebhookModule.kt`). New aggregators add their routes here.
7. `configureGrpc()` — launches gRPC server on separate coroutine (IO dispatcher) with 6 services (defined in `api/grpc/GrpcModule.kt`)
8. `configureConsumers()` — starts RabbitMQ event consumers

Environment variables: `HTTP_PORT` (default 8080), `GRPC_PORT` (default 5050),
`FREESPIN_TO_PAYOUT` (default `true` — see the spin lifecycle below).

### Sync Job (SyncJob.kt)

Standalone CLI entrypoint that syncs games from all active aggregators, then exits. Uses `startKoin` directly (not koin-ktor) with same modules minus `grpcModule`, no `Application` registration. Dispatches `SyncAllActiveAggregatorCommand` via the CQRS Bus.

**Important**: SyncJob does NOT register `Application` in Koin. A `syncOverrideModule` is loaded after `externalModule` to bind `IEventPublisherPort` to `NoOpAppEventPublisher` (sync doesn't publish events and must not open a RabbitMQ channel). If adding new singletons that depend on `Application` or the RabbitMQ `Channel`, ensure the sync code path doesn't resolve them, or add an override in `syncOverrideModule`.

## CQRS Pattern

`Bus` dispatches via `BusImpl` → `HandlerRegistry`. The registry is populated **automatically** at boot: Koin's `getAll<CqrsHandler>()` surfaces every handler, and `HandlerRegistry.register` pulls the `C`/`Q` generic type argument via Kotlin reflection. Polymorphic handlers (e.g. `SetImageCommandHandler : ICommandHandler<SetImageCommand, Unit>`) also serve concrete subtypes — lookup walks the class hierarchy when an exact match is missed, and the result is cached per concrete class.

**All write paths go through repositories.** Every `SaveXCommandHandler` loads the FK parent (e.g., `ICasinoProviderRepository.findByIdentity`), builds/merges a domain aggregate, and calls `repository.save(...)`. There are no `Exposed Table` writes inside handlers — direct writes are confined to `infrastructure/persistence/repository/*Impl`. Repository **interfaces** are defined in `domain/repository/`; **implementations** in `infrastructure/persistence/repository/`.

**Key difference**: Commands return `Result<R>` (wrapped in `runCatching`). Queries return `R` directly.

**Adding a new command/query**:
1. Define the DTO in `application/command/<feature>/X.kt` or `application/query/<feature>/X.kt`
2. Create the handler in `infrastructure/handler/<feature>/XHandler.kt`
3. Bind it with `single(named("x")) { XHandler(...) } bind CqrsHandler::class` in `HandlerModule`

That's it — `BusModule` never needs to be edited.

**Exception helpers**: `domainRequireNotNull(value) { ExceptionType() }` and `domainRequire(condition) { ExceptionType() }` throw categorized `DomainException` subclasses. Handlers and repositories **must not** throw `IllegalArgumentException` or raw `error(...)` for business rule violations — always pick an appropriate `DomainException` subclass so the gRPC interceptor maps to the right status code.

## Data Flow — Spin Lifecycle

1. **OpenCasinoSessionUsecase** — aggregator creates game adapter → gets launch URL via `getLaunchUrl(session, lobbyUrl)` → saves session → publishes `CasinoSessionEvent(session)` via `IEventPublisherPort`
2. **ProcessSpinUsecase** — for each spin (PLACE/SETTLE/ROLLBACK):
   - A freespin BET skips balance calculation entirely — a free round costs nothing
   - A freespin WIN depends on **`FREESPIN_TO_PAYOUT`** (env, default `true`): on, it is credited
     to the real balance here like any win; off, NO spin of a free round touches the wallet and the
     win reaches the outside only as a `SpinEvent`, for whoever owns the promotion to settle. crm
     already tracks `settleAmount` on the grant, so on prematch this is **off** — a bonus is settled
     once, by one owner. The spin row still carries the full `amount`; only its real/bonus split is
     zero, which is what says "no money moved here" downstream
   - Regular rounds: check player limits → calculate balance via `SpinBalanceCalculator` → withdraw/deposit via `IWalletPort` → save spin → publish `SpinEvent(spin)` (lifecycle carried by `Spin.type`, not separate event types)
3. **FinishCasinoRoundUsecase** — `round.finish()` returns the finished `CasinoRound` → save → publish `CasinoRoundEvent(round)` (with `finished = true`)

Use cases are callable via `operator fun invoke()`, return `Result<Response>`, and take domain models. They inject `IEventPublisherPort` and publish the domain model directly — the event wraps the aggregate as-is, there is no snapshot mapping — after the write commits.

**CasinoSession convenience**: `session.openRound(externalId, freespinId)` is the preferred way to create a round — it delegates to `CasinoRoundFactory` but keeps the call anchored to the parent aggregate.

**CasinoSession-command contract**: the wallet/spin entry points take a resolved `CasinoSession`, not a token. A webhook first calls `FindCasinoSessionQuery(token)` (→ `CasinoSession`, throws `CasinoSessionNotFoundException`), then dispatches `PlaceSpinCasinoSessionCommand(session, …)` / `SettleSpinCasinoSessionCommand(session, …)` / `EndCasinoRoundSessionCommand(session, …)` / `FindCasinoSessionBalanceQuery(session)`. This lets a caller override session fields per operation — e.g. TONGame, where currency isn't session-locked, passes `session.copy(currency = requestCurrency)`. The spin handlers re-bind a DB-loaded round's session to the passed one (`round.copy(session = session)`) so `ProcessSpinUsecase` uses the caller's currency. Other aggregators pass the resolved session unchanged, so behavior is identical.

## Persistence

Exposed ORM wrapped by two helpers in `infrastructure/persistence/DbTransaction.kt`:

- `dbTransaction { }` — suspended write transaction (preferred over `newSuspendedTransaction` direct)
- `dbRead { }` — read-only transaction for query handlers and `find*` repository methods

Nothing outside `DbTransaction.kt` should import `newSuspendedTransaction` directly.

Entity ↔ domain conversion via mapper extension functions (`object XMapper { fun XEntity.toDomain(): X }`). ResultRow extensions use distinct names (`toCasinoProvider`, `toAggregator`, etc.) to avoid `toDomain` collisions when one mapper composes another — see `.claude/rules/mapper-conventions.md`.

- **Long PK tables** (`LongIdTable`): CasinoSessionTable, CasinoRoundTable, SpinTable, CasinoGameTable, CasinoGameVariantTable, CasinoProviderTable, AggregatorTable, CollectionTable, CasinoGameCollectionTable, CasinoGameFavouriteTable, SportbookSessionTable, BetTable
- **New entity detection**: `id == Long.MIN_VALUE` means unsaved
- **JSON columns**: `config`, `tags`, `images`, `locales`, `platforms`, `name` (LocaleName) via `kotlinx.serialization`

Repository methods raise domain exceptions on FK violations (`CasinoProviderNotFoundException`, `AggregatorNotFoundException`, `CasinoGameNotFoundException`, `CollectionNotFoundException`) via `domainRequireNotNull`. Image updates flow through `ICasinoGameRepository.addImage(identity, key, url)` (and the analogous methods on provider/collection) — handlers do not touch entity DAOs directly.

See `.claude/rules/exposed-database.md` for detailed Exposed ORM conventions.

### Catalog search (fuzzy, typo-tolerant)

Every `query` string on a listing (`CasinoGameFilter.query`, `FindAllCasinoProviderQuery.query`,
`FindAllCollectionQuery.query`, `FindAllAggregatorQuery.query`) goes through
`infrastructure/persistence/search/SearchIndex.matches(...)` instead of a `LIKE '%q%'`. Three OR-ed
branches, all served by the GIN indexes of `V11__fuzzy_search.sql`:

1. **every token** of the query is a substring of the row's searchable text *or* trigram-similar to
   one of its words (pg_trgm `<%`). Tokens are ANDed, so word order and missing words don't matter —
   `gates olimpus` finds *Gates of Olympus*, `bonanca` finds *Sweet Bonanza*;
2. the **whole phrase** is trigram-similar, covering a query typed without spaces (`bookofra`);
3. the query's **metaphone** codes are all present in the row's codes, which catches what trigrams
   give up on — `rulet` → *Roulette*, `gaets` → *Gates*, `krown` → *Crown*. Codes are taken at full
   length (`metaphone(word, 16)`), never `dmetaphone`: its four-character code collapses
   *startbust*, *street* and *stardom* onto one key and answers a search with everything but the
   game asked for. Changing the code function means **rebuilding the phonetic indexes** — replacing
   an `IMMUTABLE` body leaves them holding the old codes (see `V14__phonetic_full_code.sql`).

When those three answer **nothing at all**, `searchPass` retries once with a fourth, deliberately
generous branch (`V12__fuzzy_search_fallback.sql`): any word of the row that *starts* within a few
edits of the typed word — `startbust` → *Starburst*, `blakj` → *Blackjack*, `ruletka` → *Roulette*.
It is a Levenshtein scan with no index behind it, which is exactly why it is a fallback: a player
whose query the catalog recognises never pays for it and never sees its noise, and a player who
mistyped badly gets the closest games instead of an empty screen. Handlers reach it through
`searchPass(relaxable, condition, count)` — strict condition, count, and only on zero the wider one.

`SearchIndex.relevanceOrdering(query)` leads a searched listing with exact-prefix > exact-fragment >
*sounds like it* > loose trigram resemblance, with the trigram score deciding inside each band. The
phonetic band earns its place: without it `gaets` ranked *Gaelic Warrior* — trigram noise — above
*Gates of Olympus*, the row the query was actually about. The curated `sort_order` / rail position
stays as the tiebreaker, and the array is empty when nothing was typed, so an unsearched listing is
unchanged.

`SearchIndexes` (one entry per listing) is the **mirror of the expression indexes in
`V11__fuzzy_search.sql`** — Postgres matches an expression index structurally, so adding a column on
one side without the other silently degrades that search to a sequential scan. The two SQL helpers
(`casino_search_norm`, `casino_search_phonetic`) need the `pg_trgm` and `fuzzystrmatch` extensions,
created by the same migration (both are *trusted* since PG13, so the database owner suffices).
The `<%` threshold is set **on the database** by `V13__search_threshold.sql`, not per connection —
routing it through Hikari's `connectionInitSql` leaves an uncommitted transaction on every fresh
pooled connection (`isolateInternalQueries` is false by default) and every query on that connection
then fails with *"Cannot change transaction isolation level in the middle of a transaction"*.

## Proto / gRPC

Proto files in `src/main/proto/game/v1/`. Package: `game.v1` (Java: `com.nekgamebling.game.v1`).

- DTOs in `dto/` subdirectory as `<name>.dto.proto` (see `.claude/rules/proto-dto.md`)
- Services in `service/` subdirectory: CasinoGameService, CollectionService, CasinoProviderService, AggregatorService, FreespinService, WinnerService, SportbookService
- Full gRPC client API reference: `src/main/proto/API.md`

Each gRPC service extends `*GrpcKt.*CoroutineImplBase`, takes `Bus` as constructor parameter, and wraps every method in `handleGrpcCall { }` which maps `DomainException` → gRPC status codes and stores the exception class name in an `x-exception-name` metadata header for downstream error identification.

**Exception → Status mapping**: `NotFoundException` → `NOT_FOUND`, `BadRequestException` → `INVALID_ARGUMENT`, `ConflictException` → `ALREADY_EXISTS`, `ForbiddenException` → `PERMISSION_DENIED`, `SystemException` → `INTERNAL`.

**Name collision**: Proto and CQRS classes share names (e.g., `SaveCasinoGameCommand`). Use Kotlin import aliases:
```kotlin
import com.nekgamebling.game.v1.SaveCasinoGameCommand as SaveCasinoGameProto
import application.cqrs.game.SaveCasinoGameCommand as SaveCasinoGameCqrs
```

## Aggregator Integration

Currently implemented: **OneGameHub** (`infrastructure/aggregator/onegamehub/`), **Pragmatic Play** (`infrastructure/aggregator/pragmatic/`), **Pateplay** (`infrastructure/aggregator/pateplay/`), **TONGame** (`infrastructure/aggregator/tongame/`), and **Gambutsoft** (`infrastructure/aggregator/gambutsoft/`). In progress: **01.tech Sport** (`infrastructure/aggregator/tech01sport/`, sportsbook — package name is `tech01sport` because a Kotlin package segment cannot start with a digit; integration string `"01TECHSPORT"`). Has `Tech01SportConfig`, `Tech01SportAdapterProvider` (casino factory methods throw `CasinoNotSupportedException`), `Tech01SportbookAdapter` (`ISportbookPort.open(session)` — stateless, NO Redis: returns `{token: session.token, partnerId, apiUrl, currency}` (apiUrl = SDK backend base; currency = the session currency the frontend passes into the SDK)), and `Tech01SportWebhook` — 7 POST routes under `/api/webhook/tech01sport`, all HTTP 200 with body codes (`Tech01SportCode`), all verifying the `betting-signature` HMAC (`Tech01SportSignatureVerifier`, any of `config.secretKeys` may match; timestamp must be within ±300s of server time): `/ping`; `/create-private-token` (session by public token → `ExchangeSportbookTokenCommand` mints UUID into `externalToken`; one-time, reuse answers 1); `/get-user` (currency from the player's LAST sportbook session — no session → opt-in code 12 UserNotFound; wallets from `IWalletPort.findBalance`: typeId 1 real / 2 bonus + `activeCurrencyCode`); `/prepare-credit-bet` (session by private token; `currencyCode` must equal the session currency, else code 5 — a session works ONLY in its opening currency, settlements/rollbacks enforce `bet.currency` via `BetCurrencyMismatchException` → `PlaceBetCommand`: wallet hold + `Bet` created with `externalId = transactionId`, insufficient → 2); `/credit-bet` (`ConfirmBetCommand`: rebinds `externalId` from placement transactionId to `bet.id`, fills type/selections); `/debit-bet-by-batch` (`SettleBetCommand` per item → `SettleBetResult{bet, debt}`: wallet move by sign of `amount`, status from `bet.status == 2`, unknown bet → 3; a clawback the balance cannot cover drains it and reports the shortfall as Betting-managed `debt` in the item — THEY store and recover it, we track nothing); `/rollback-bet-by-batch` (`RollbackBetCommand`: refund + bet deleted, unknown transaction → 7). Money: signed decimal strings ↔ nano via `Tech01SportMoney` (scale 9). Bet flows go through `ProcessBetUsecase` (place/confirm/settle/rollback — publishes `BetEvent` after commit; wallet ops keyed `sportbook:<phase>:<txId>` rely on wallet-side idempotency). Fortune Wheel routes `/credit` (stake by private token), `/debit-by-batch` (payout by userId), `/rollback-by-batch` (refund, unknown tx → 7) go through `ProcessWheelUsecase` — pure wallet passthrough, deliberately OUTSIDE the Bet model, no domain aggregate and no event; replay/ordering safety via `IWebhookGuardPort` (stake claims `sportbook:wheel:tx:<id>`, rollback marks it rolled back first so a late stake is refused, rollback of a never-claimed tx answers 7 instead of minting money).

**Gambutsoft specifics** (vendor docs: Gamble Hub / games-hub.net; integration string `"GAMBUTSOFT"`, aggregator row identity `gambutsoft`). Seamless REST aggregator split across two vendor hosts. **Outbound**: `GambutsoftHttpClient` calls `POST {officeApiUrl}/auth/login` (form-urlencoded, NOT JSON — JSON answers 400/401) for a Bearer JWT, `GET {officeApiUrl}/users/{userId}/getUserGames/{currencyISO}` for the catalogue, and `POST {clientApiUrl}/games/openGame` for a session. **The catalogue is per currency**: `GambutsoftGameAdapter.getAggregatorGames()` unions every currency in `catalogCurrencies` (falling back to the currencies the login response reports), filters `isEnabled`, and de-duplicates by id — the union must happen inside the adapter, because `SyncAggregatorUsecase` derives provider matching from the whole feed and a truncated feed mints duplicate providers. A currency the account does not hold answers HTTP 400 and fails the sync loudly. Game ids are compound (`black:pragmatic:1005`) and travel back verbatim as `openGame.gameId`, so they are never split; the middle segment is the vendor's stable provider slug and is the fallback when the display `provider` is blank. The feed carries no platforms/locales/features, so variants default to `DESKTOP + MOBILE` (never empty — `CasinoSessionFactory` hard-requires the platform), no locales, and the undocumented `category` field becomes a tag. **The provider mints the session id**: `openGame` returns `content.gameRes.sessionId`, persisted as `CasinoSession.externalToken`, and it is the ONLY key inbound callbacks carry — `player_login` sends our `playerId` (their reporting stays truthful) and is a cross-check, never a lookup key. `exitUrl` is required by the vendor while `PlayCasinoGameCommandHandler` passes an empty lobby url, so `config.exitUrl` is what real play uses. Demo is supported (`demo = "1"`, `config.demoLogin`). **Freespins are not**: the vendor exposes no freespin API and the unit of `openGame.freespinTotalBet` is unstated, so `GambutsoftFreespinAdapter` throws `FreespinNotSupportedException` and the catalogue reports `freeSpinEnable = false`. **Inbound**: `GambutsoftWebhook` — ONE `POST /api/webhook/gambutsoft` serving `getBalance` / `writeBet` / `rollback`, told apart by the body's `cmd` field (the vendor configures a single callback URL per currency). Every request is authenticated by `X-Signature` = lowercase hex HMAC-SHA256 over the RAW body bytes with `config.secretKey` — the same key signs our outbound `openGame`, and the body is read as text before parsing because the hash covers the exact bytes. Their scheme has **no nonce and no timestamp**, so replay safety comes only from `transactionId`. One `writeBet` may carry `bet` and `win` at once: both legs run under `IWebhookGuardPort.withLock("gambutsoft:wallet:<playerId>:<currency>")`, **PLACE strictly first** (the only leg with an affordability gate — netting them into one spin breaks affordability, real/bonus attribution, rollback, RTP, limits and the crm event contract), spin ids are `<transactionId>:place` / `<transactionId>:settle`, and the round id is `roundId ?: transactionId`. `round_finished` closes the round after the spins, non-fatally. `rollback` marks `gambutsoft:<transactionId>` rolled back FIRST (a late `writeBet` for it is then refused), then reverses `[<tx>:settle, <tx>:place]`; a repeat rollback answers 200 with the current balance. **Money is MAJOR units** on the wire (`bet: 25` is 25.00) and arrives as a JSON number OR a string depending on the sub-vendor — `GambutsoftMoney` converts to/from nano through `BigDecimal` (truncating DOWN, negatives refused by `Amount`) and emits `balance` as an unquoted JSON number. Accepted = HTTP 200 + `status: "success"`; declined = HTTP 400 + `status: "fail"` (the vendor treats any non-2xx as retryable), and a decline still reports the real balance so the game does not display an empty wallet. Config keys: `officeApiUrl`, `clientApiUrl`, `login`, `password`, `secretKey`, `userId`, `callbackUrl`, `exitUrl`, `catalogCurrencies`, `language`, `demoLogin`. Their hosts sit behind a Cloudflare managed challenge that answers HTML to unrecognised clients, so the client sends an explicit User-Agent and **their allowlist must carry our NAT egress IP** — nothing works from a laptop or CI.

Routing is handled by `AggregatorRegistry : IAggregatorFactory` (in `infrastructure/aggregator/`). It indexes every bound `AggregatorAdapterProvider` by its `integration` string and raises `AggregatorNotSupportedException` for unknown keys. Each aggregator provides: Config, `*AdapterProvider` (implementing `AggregatorAdapterProvider` — replaces the old `*AdapterFactory`), CasinoGameAdapter (ICasinoGamePort), FreespinAdapter (IFreespinPort), HttpClient, and Webhook.

**Adding a new aggregator is one new file + one Koin line.** Create `<Name>AdapterProvider` with an `integration` string and factory methods, then in `ExternalModule`:
```kotlin
single(named("<name>")) { <Name>AdapterProvider() } bind AggregatorAdapterProvider::class
```
The registry picks it up through `getAll<AggregatorAdapterProvider>()` at boot — no edits to `AggregatorRegistry` or any existing code.

See `.claude/skills/add-aggregator.md` for the step-by-step guide. See `.claude/agents/seed-collections.md` for the collection seeding agent.

**GamingFlow free rounds**: ONE free round arrives as SEVERAL `withdrawAndDeposit` calls sharing a
`gameRoundRef` — the spin that charges a round (`chargeFreerounds: 1`, a non-zero `withdraw`), its
tumbles, and the collect that pays the win (`deposit > 0`). Only the FIRST carries the charge; the rest
say `freeround: true`, and `bonusId` rides on all of them, including plain `getBalance`. So the marker
for "this call belongs to the grant" is `chargeFreerounds > 0 || freeround`, never `bonusId` alone (a
paid spin in a bonus session carries it too and its stake must still be taken) and never the charge
alone (that loses the win, which then settles as ordinary money). The charged spin IS recorded as a
PLACE carrying the grant — keeping it off the wallet is `ProcessSpinUsecase`'s job, and dropping the
spin instead would leave the promotion's owner with no sign a round was spent.

**Pragmatic specifics**: Uses MD5 hash authentication (sorted params + secret key), form-encoded POST requests, GET webhook endpoints at `/pragmatic/*.html`, and decimal string amounts (converted to/from minor units via ×100).

**Pateplay specifics**: Static game catalog (no game discovery API), launch URLs constructed locally (no API call), HMAC-SHA256 authentication for freespin API, no webhook handler (wallet callback not yet implemented).

**TONGame specifics**: REST aggregator (HTTP/JSON, no gRPC/protobuf). Two directions:

- **Outbound (we → provider)**: `TongameHttpClient` (`infrastructure/aggregator/tongame/client/`, Ktor CIO, `expectSuccess=true`) calls `GET <apiUrl>/api/v1/games` and `POST <apiUrl>/api/v1/session` with `X-Operator` + `X-Secret-Key` headers. `getAggregatorGames()` maps the game list (provider supplies only `identity`; `name` defaults to `identity`, locales/platforms defaulted) → `AggregatorGame`. **We mint the session token; the provider mints nothing**: `getLaunchUrl()` calls `POST /api/v1/session {token}` sending our own `session.token`, and embeds that same token in the launch URL. The provider then calls our `/player` webhook with the token to learn the player, and echoes the token back as `sessionToken` in every wallet webhook, so each resolves via our `findByToken`. Freespins unsupported.
- **Inbound (provider → us)**: aggregator **webhook** like `OneGameHubWebhook`/`PragmaticWebhook`. `TongameWebhook` (`infrastructure/aggregator/tongame/webhook/`) exposes six flat POST routes under `/api/webhook/tongame` — `/player`, `/balance`, `/round/open`, `/round/close`, `/debit`, `/credit` — bound in `aggregatorModule` and wired into `api/webhook/WebhookModule.kt`'s `configureWebhook()` (all webhooks share the Ktor HTTP server; **no separate gRPC server**). The registered `webhookUrl` is `<our-host>/api/webhook/tongame`. Every request carries `sessionToken` = our own `session.token`, so each route resolves **our** session via `FindCasinoSessionQuery(sessionToken)` (findByToken) and verifies the `X-Secret-Key` header against the aggregator's stored `apiKey` (read off `session.gameVariant.game.provider.aggregator.config`). It then bridges into the spin pipeline via `Bus`: `/player`→resolve session→`IPlayerPort.findPlayer(session.playerId)` (pam-engine `UserService.Find`) → `{id, username, profilePic}` (`id` = our `session.playerId`, which the provider stores as its `player.externalId`), `/balance`→`IWalletPort.findBalance(session.playerId, currency)` → `{balance}`, `/debit`→`PlaceSpinCasinoSessionCommand`, `/credit`→`SettleSpinCasinoSessionCommand`, `/round/close`→`EndCasinoRoundSessionCommand`, `/round/open`→`200` (round opens lazily on first `/debit`). **Currency is not locked to the session for TONGame** — the player can switch currency in-game, so every wallet call carries its own `currency`. The webhook pins it onto the resolved session (`session.copy(currency = …)`) before dispatching (see the session-command contract below). Spin ids are `<roundId>:place|settle:<transactionId>` (the provider's per-tx id — a round may carry many place/settle pairs, e.g. plinko multi-drop); legacy senders without `transactionId` fall back to `<roundId>:place|settle`. Money on the wire is **integer minor units == the wallet's system units (nano)**, passed straight through (no `ICurrencyPort` conversion). Domain exceptions map to HTTP status: `CasinoSessionNotFound`/bad `X-Secret-Key`→`401`, `InsufficientBalance`/`MaxPlaceSpin`→`402` (the `/debit` decline path), `Forbidden`/`NotFound`/`Conflict`→`409`; anything else propagates (→500).

Launch URLs put the game in a subdomain (`<gameSymbol>.<gameHost>`) and carry the three query params the provider's game client reads: `?sessionToken=<our-token>&currency=<currency>&operator=<operatorIdentity>` (the client replays `sessionToken` + `operator` in its WS `auth` frame so the provider resolves our session by `(token, operator)`; no `mode` param). **TONGame has no demo mode** — `getDemoUrl` throws `DemoNotSupportedException` (games are published with `demoEnable=false`). Config keys: `apiUrl` (provider REST base, e.g. `https://provider.example.com`), `operatorIdentity` (sent as `X-Operator`), `apiKey` (the shared secret — sent as `X-Secret-Key` and verified on inbound webhooks), `gameHost`. Player profiles for `/player` come from pam-engine over gRPC (`PamAdapter`/`IPlayerPort`, env `PAM_GRPC_HOST`/`PAM_GRPC_PORT`).

## Event System (AppEvent envelope → RabbitMQ)

**Uniform envelope, per-model events, published AS-IS.** Every event ships as `{ "playerId": <key>, "data": {<domain model>} }` on a `<domain>.events` route. The `data` is the **domain aggregate itself** — `SpinEvent` wraps `domain.model.Spin`, `CasinoRoundEvent` wraps `CasinoRound`, `CasinoSessionEvent` wraps `CasinoSession`. There are **no snapshot twins and no mappers** (the old `event/model/` + `event/mapper/` were deleted). The domain models are `@Serializable`, so the full nested aggregate graph (e.g. `Spin.round.session.gameVariant.game.provider.aggregator` — including `aggregator.config` secrets — plus the recursive `reference`) ships verbatim. One event per domain model; lifecycle lives *inside* the model (e.g. `Spin.type`), never as separate `*Placed`/`*Settled` types.

The event types + contract live in **`domain/event/`**; the RabbitMQ machinery lives in **`infrastructure/rabbitmq/`**:
- `domain/event/AppEvent.kt` — the `AppEvent<T>` interface + its `Meta<T>` companion contract (`route` + `serializer` + `create(data)`). Pure domain, no framework deps.
- `domain/event/SpinEvent.kt` / `CasinoRoundEvent.kt` / `CasinoSessionEvent.kt` — one file per event; each takes one `data: <domain model>`, derives `playerId` from `data` (e.g. `data.round.session.playerId.value`), and a companion `Meta`.
- `application/port/external/IEventPublisherPort` — the driven port use cases depend on (`publish(AppEvent<*>)`); follows the project `I…Port` convention.
- `infrastructure/rabbitmq/AppEventBus.kt` — `RabbitAppEventPublisher` (impl of `IEventPublisherPort`; owns `appJson` + channel + envelope), `NoOpAppEventPublisher`, the generic `AppEventConsumer<E>` base, `EVENT_EXCHANGE` (env, default `crm.exchange`), `declareEventExchange(channel)`.
- `domain/util/AnyMapSerializer.kt` — custom `KSerializer<Map<String,Any>>` for `Aggregator.config` (kotlinx has no serializer for `Any`); without it the full graph would not compile.

**Routes:** `spin.events`, `round.events`, `session.events`. `SpinType` serializes as the domain enum names `PLACE`/`SETTLE`/`ROLLBACK` (NOT `Place`/`Settle`/`Rollback`); round-finished is `CasinoRoundEvent` with `finished = true` (NOT a spin type). The only live consumer is **crm-engine** (bonus-engine is decommissioned) — the wire shape is now the full domain graph, so crm ingestion must tolerate it.

**No codec outside the adapter.** Use cases inject `IEventPublisherPort` and call `publish(SpinEvent(spin))` etc. `RabbitAppEventPublisher` wraps the model in the envelope and publishes on `meta.route`. No use case or event touches JSON, bytes, or the channel.

**Consumer**: `PlaceSpinEventConsumer : AppEventConsumer<SpinEvent>(channel, SpinEvent::class)` (in `infrastructure/rabbitmq/`) is a read-only router — `handle()` checks `spin.type == SpinType.PLACE` and delegates the Redis limit decrement to `DecreasePlayerLimitUsecase`, reading `spin.round.session.playerId` + `spin.amount` straight off the domain model. No business logic, never publishes. Its queue name is auto-derived from the class `simpleName`; the base `init` declares + binds the queue and starts consuming. The delivery callback decodes the envelope and runs `handle()` via `runBlocking`, **wrapped in try/catch** so a poison/failed delivery can never close the shared channel (the 2026-06-09 outage — do NOT remove the try/catch). Auto-ack is on; a failed `handle()` is logged, not requeued.

**Connection & channels**: `infrastructure/rabbitmq/rabbitMqConnection(config)` opens a single `com.rabbitmq.client.Connection` from `RabbitMqConfig` (bound as `single<Connection>` in `ExternalModule`). The publisher and consumers run on SPLIT channels from that connection: `RabbitAppEventPublisher` lazily owns a dedicated confirm-mode channel (`confirmSelect()`, PERSISTENT deliveries, `waitForConfirmsOrDie` 5s, publishes serialized under a lock; a channel closed by an AMQP error is re-created and the publish retried once), while the `single<Channel>` binding backs the consumers + topology declaration only — a poison delivery can no longer touch the publisher path.

**Publishing timing**: usecases publish **after** the DB transaction commits (outside the `dbTransaction { }` block) so a failed write never emits phantom events. See `.claude/rules/domain-events.md`. `ProcessSpinUsecase` awaits the wallet debit/credit and publishes `SpinEvent` once the spin row commits. A failed wallet move fails the whole spin: nothing in this service reconciles a movement that did not land, so the spin row is never written, no event is published, and the call answers `INTERNAL` — the hub retries under the same leg id, on which the wallet's `reference` is idempotent.

**Connection config**: `RabbitMqConfig` (built in `infrastructure/koin/ConfigModule.kt`) reads `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`, `RABBIT_PASSWORD`, and `RABBIT_TLS` from env. `RABBIT_TLS=true` switches the URI scheme to `amqps://` — required for AWS Amazon MQ for RabbitMQ and any TLS-only broker. The Java client auto-enables TLS via the URI scheme using the JVM's default trust store (publicly-signed CAs), so no keystore is needed for AWS. Set `RABBIT_PORT=5671` alongside `RABBIT_TLS=true`; the default port is not changed automatically.

## Koin Dependency Injection

**Module install order matters** — dependencies must be installed before dependents.

**Main server** (`infrastructure/koin/KoinInit.kt`): Registers `Application` instance first, then 8 modules:
`configModule → persistenceModule → externalModule → usecaseModule → handlerModule → busModule → aggregatorModule → grpcModule`

The `grpcModule` is defined in `api/grpc/config/` and registers gRPC service singletons. All other modules are in `infrastructure/koin/`.

**`HandlerModule`**: every handler is declared as `single(named("<x>")) { ... } bind CqrsHandler::class`. The named qualifier is required because Koin rejects duplicate `single`s of the same type when binding to a common supertype; the marker binding is what allows `busModule` to `getAll<CqrsHandler>()` in one call.

**`BusModule`**: tiny (~13 lines). It constructs a `HandlerRegistry`, populates it from `getAll<CqrsHandler>()`, and wraps the result in `BusImpl`. Never needs to be touched when adding handlers.

**`ExternalModule`**: `AggregatorAdapterProvider`s are bound with named qualifiers and `bind AggregatorAdapterProvider::class` so `AggregatorRegistry(providers = getAll())` collects them all. It also binds `single<Connection> { rabbitMqConnection(get()) }`, `single<Channel> { get<Connection>().createChannel() }` (consumer/topology channel), `single<IEventPublisherPort> { RabbitAppEventPublisher(connection = get()) }`, and the `PlaceSpinEventConsumer`.

**SyncJob** (SyncJob.kt): Same modules minus `grpcModule`, no `Application` registration, includes `syncOverrideModule` which binds `single<IEventPublisherPort> { NoOpAppEventPublisher }` so sync never publishes events or opens a RabbitMQ channel.

**Application registration**: the `Application` instance is registered in `configureKoin()` as `module { single { application } }` (koin-ktor does not auto-register it) for the webhook/gRPC layers. The event publisher and consumer no longer depend on `Application` — they take a `com.rabbitmq.client.Channel`.

## Key Design Decisions

- **Value objects**: `@JvmInline value class` with `init` block validation via `domainRequire(...)`; validation errors are `DomainException` subclasses, not `IllegalArgumentException`
- **Amount**: wraps `Long` in minor units (cents) with operator overloads; `Amount.ZERO` constant; `minOf(Amount, Amount)` top-level helper
- **Domain traits** (Activatable, Imageable, Orderable): mutable interfaces. Game overrides via `copy()` for immutability; Provider/Collection/Aggregator mutate directly
- **Aggregator type** (sportsbook prep): `Aggregator.type: AggregatorType` enum (`CASINO`/`SPORTBOOK`, default `CASINO`); `CasinoProvider` init enforces a `CASINO` aggregator via `domainRequire` → `UnsupportedAggregatorTypeException`. Wired end-to-end: `AggregatorTable.type` (`enumerationByName`, Flyway `V6__aggregator_type.sql`), proto `AggregatorTypeDto` on `AggregatorDto`/`SaveAggregatorCommand` (`UNSPECIFIED` maps to `CASINO`)
- **Sportsbook Bet**: flat single-aggregate model (deliberately NO Round/Spin analog, no transaction log — owner decision, do not add one): `Bet {externalId (globally unique — only one sportsbook aggregator is active by design), playerId, session: SportbookSession, currency, betAmount, winAmount, type SINGLE/COMBO/SYSTEM, status OPEN/WON/LOST, selections (JSON, `BetSelection` match info), createdAt/updatedAt}`. No coefficient stored. Settlement/re-settlement = lookup by `externalId`, overwrite `status`+`winAmount` (absolute values → naturally idempotent). Terms are `betAmount`/`winAmount`, NOT place/settle (avoids casino confusion). `BetEvent` → `bet.events`; `IBetRepository` (eager-loads `session.aggregator`)
- **Sportsbook open flow**: gRPC `SportbookService.Open(player_id, currency)` → `OpenSportbookCommand` → `OpenSportbookUsecase` → finds the first ACTIVE `SPORTBOOK` aggregator (`IAggregatorRepository.findFirstActiveByType`), resolves `ISportbookPort` via `IAggregatorFactory.createSportbookAdapter(aggregator)` (default on `AggregatorAdapterProvider` throws `SportbookNotSupportedException` — casino providers untouched), takes `(playerId, currency)`, mints `token` (base24, same generator as casino sessions), calls `adapter.open(session)` BEFORE the DB write, persists `SportbookSession {token (unique), externalToken (private token, null until exchanged), playerId, currency, aggregator, data}` via `ISportbookSessionRepository` (`sportbook_sessions`; `findByToken` resolves inbound webhooks; `aggregator.integration` tells the frontend which SDK to load, `data` is the adapter's init payload), publishes `SportbookOpenEvent` → `sportbook.events` after the commit and returns the session. Flyway `V9__sportbook.sql` creates `sportbook_sessions` + `bets`
- **Casino prefix** (sportsbook prep): the casino bounded context is prefixed `Casino*` everywhere — `CasinoGame`/`CasinoProvider`/`CasinoGameVariant`/`CasinoSession`/`CasinoRound` and every derived type (repos, commands/queries, handlers, tables, entities, mappers, proto messages/services, exceptions). DB tables renamed with the `casino_` prefix in Flyway `V7__casino_prefix_rename.sql` (game/provider) and `V8__casino_prefix_sessions_rounds.sql` (sessions/rounds). Property/JSON field names (`game`, `provider`, `gameVariant`, `session`, `round`), routing keys (`session.events`, `round.events`) and the event wire shape are UNCHANGED; vendor/aggregator-internal types (`AggregatorGame`, vendor `GameDto`s, protocol strings like GamingFlow `"Game.List"`) keep their names. Proto package stays `game.v1`; the message/service renames are a breaking change for `game-grpc-client` consumers.
- **Monetary values**: `Long` in minor units internally, `string` in proto for BigInteger precision
- **Factories**: `object` singletons with validation (e.g., `CasinoSessionFactory.create()` checks active status and locale/platform support); `CasinoSession.openRound()` delegates to `CasinoRoundFactory` as a convenience on the parent aggregate
- **SpinBalanceCalculator**: PLACE deducts (real-first when bonusBet), SETTLE deposits to same pool as original bet, ROLLBACK refunds to original pools. `canAfford` gates PLACE only — SETTLE/ROLLBACK credit the player and are never declined by balance. Exhaustively unit-tested.
- **Spin convenience**: `spin.isPlace` / `isSettle` / `isRollback` computed properties (getter-only — kotlinx serializes only constructor state, so they never appear on the wire). `SpinEvent(spin)` publishes the domain `Spin` directly; `SpinType` serializes as `PLACE`/`SETTLE`/`ROLLBACK`
- **CasinoRound.finish()**: returns the finished `CasinoRound` (sets `finishedAt`); `FinishCasinoRoundUsecase` publishes `CasinoRoundEvent(round)` with `finished = true` after the write commits
- **Read-side projections**: query handlers that join across aggregates return `application/projection/<ctx>/<X>Projection` DTOs (e.g. `CollectionProjection` with game counts), never polluting domain models with denormalized fields
- **Wallet dependency**: the wallet is pam-engine. `com.nekgambling:pam-grpc-client` (GitHub Packages, `IGaming-Pam-Engine`) carries the player account, the ledger and the currency registry in one artifact, and `PamAdapter`/`WalletAdapter`/`CurrencyAdapter` in `infrastructure/pam/` share ONE channel on `PAM_GRPC_HOST`/`PAM_GRPC_PORT`. Money moves through `WalletService.Transact` — signed nano, idempotent by `reference`, addressed by `account_id` (cached per player+currency, minted on demand with `EnsureAccount`). The reference must name a single movement: `ProcessSpinUsecase` keys it `spin:<type>:<externalId>`, the sportbook paths `sportbook:<phase>:<txId>`.
- **Images are URLs, not files**: the engine never touches file content or object storage. `Update*Image` RPCs carry a full public URL; callers (backoffice/admin apps) upload to S3 themselves and construct the URL from their CDN host env.

## CI/CD

GitHub Actions workflow (`publish-grpc-client.yml`) publishes `com.nekgamebling:game-grpc-client` to GitHub Packages on bare-semver tag push (`1.0.0`, no `v` prefix) or manual dispatch. Version can be overridden with `-PgrpcClientVersion=x.y.z`. `publish-grpc-clients.yml` (same tag trigger) publishes the TS client `@nekzabirov/game-grpc-client` + raw protos `@nekzabirov/game-proto` via the shared `IGaming-gRPC-Actions` workflow.
