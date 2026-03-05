# Game Core gRPC API Reference

Complete API reference for the Game Core gRPC services.

**Protocol:** gRPC
**Package:** `game.service`
**Java Package:** `com.nekgamebling.game.service`

---

## Table of Contents

- [Services](#services)
  - [GameService](#gameservice)
  - [RoundService](#roundservice)
  - [ProviderService](#providerservice)
  - [CollectionService](#collectionservice)
  - [AggregatorService](#aggregatorservice)
- [Enums](#enums)
- [Common DTOs](#common-dtos)
- [Error Handling](#error-handling)
  - [Error Codes](#error-codes)
  - [gRPC Status Mapping](#grpc-status-mapping)
  - [Error Metadata](#error-metadata)
  - [Error Types Reference](#error-types-reference)
  - [Client Examples](#client-examples)

---

## Services

### GameService

Game management, session handling, and freespin operations.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindGameQuery` | `FindGameResult` | Get a single game by identity |
| `FindAll` | `FindAllGameQuery` | `FindAllGameResult` | List games with filters and pagination |
| `Play` | `PlayGameCommand` | `PlayGameResult` | Open a game session (real money) |
| `DemoUrl` | `GameDemoUrlQuery` | `GameDemoUrlResult` | Get demo URL (no real money) |
| `Update` | `UpdateGameCommand` | `UpdateGameResult` | Update game configuration |
| `UpdateImage` | `UpdateGameImageCommand` | `UpdateGameImageResult` | Upload/update game image |
| `AddTag` | `AddGameTagCommand` | `AddGameTagResult` | Add tag to a game |
| `RemoveTag` | `RemoveGameTagCommand` | `RemoveGameTagResult` | Remove tag from a game |
| `GetFreespinPreset` | `GetFreespinPresetQuery` | `GetFreespinPresetResult` | Get freespin preset options |
| `CreateFreespin` | `CreateFreespinCommand` | `CreateFreespinResult` | Create a freespin for player |
| `CancelFreespin` | `CancelFreespinCommand` | `CancelFreespinResult` | Cancel a freespin |

---

#### Find

Get a single game with full details including provider, variant, and aggregator info.

**Request: `FindGameQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Unique game identifier |

**Response: `FindGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `game` | `GameDto` | Game entity |
| `provider` | `ProviderDto` | Provider details |
| `active_variant` | `GameVariantDto` | Active game variant |
| `aggregator` | `AggregatorInfoDto` | Aggregator configuration |
| `collections` | `CollectionDto[]` | Collections containing this game |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game with specified identity does not exist |

---

#### FindAll

List games with pagination and filters.

**Request: `FindAllGameQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by name or identity |
| `active` | `bool` | No | Filter by active status |
| `provider_identities` | `string[]` | No | Filter by provider identities |
| `collection_identities` | `string[]` | No | Filter by collection identities |
| `tags` | `string[]` | No | Filter by tags |
| `bonus_bet_enable` | `bool` | No | Filter by bonus bet support |
| `bonus_wagering_enable` | `bool` | No | Filter by bonus wagering support |
| `free_spin_enable` | `bool` | No | Filter by freespin support |
| `free_chip_enable` | `bool` | No | Filter by free chip support |
| `jackpot_enable` | `bool` | No | Filter by jackpot support |

**Response: `FindAllGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `GameItemDto[]` | List of game items |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `providers` | `ProviderDto[]` | Related providers from results |
| `aggregators` | `AggregatorInfoDto[]` | Related aggregators from results |
| `collections` | `CollectionDto[]` | Related collections from results |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid pagination parameters |

---

#### Play

Open a game session for real money play. Returns a launch URL.

**Request: `PlayGameCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `player_id` | `string` | Yes | Player identifier |
| `currency` | `string` | Yes | Currency code (e.g., "USD", "EUR") |
| `locale` | `string` | Yes | Locale code (e.g., "en", "de") |
| `platform` | `PlatformDto` | Yes | Platform type |
| `lobby_url` | `string` | Yes | URL to return to lobby |
| `spin_max_amount` | `int64` | No | Max spin amount for this player (cached with 1h TTL) |

**Response: `PlayGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `launch_url` | `string` | URL to launch the game |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `GAME_UNAVAILABLE` (4000) | `UNAVAILABLE` | Game is disabled or unavailable |
| `SESSION_INVALID` (3000) | `UNAUTHENTICATED` | Session creation failed |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | Game's aggregator not supported |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | Aggregator returned an error |
| `EXTERNAL_SERVICE_ERROR` (6000) | `UNAVAILABLE` | External service (wallet, etc.) failed |

---

#### DemoUrl

Get a demo URL for playing without real money.

**Request: `GameDemoUrlQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `currency` | `string` | Yes | Currency code |
| `locale` | `string` | Yes | Locale code |
| `platform` | `PlatformDto` | Yes | Platform type |
| `lobby_url` | `string` | Yes | URL to return to lobby |

**Response: `GameDemoUrlResult`**

| Field | Type | Description |
|-------|------|-------------|
| `launch_url` | `string` | Demo game URL |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `GAME_UNAVAILABLE` (4000) | `UNAVAILABLE` | Game or demo mode not available |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | Aggregator not supported |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | Aggregator returned an error |

---

#### Update

Update game configuration.

**Request: `UpdateGameCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `bonus_bet_enable` | `bool` | No | Enable/disable bonus bet |
| `bonus_wagering_enable` | `bool` | No | Enable/disable bonus wagering |
| `active` | `bool` | No | Enable/disable game |

**Response: `UpdateGameResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid field values |

---

#### UpdateImage

Upload or update a game image.

**Request: `UpdateGameImageCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `key` | `string` | Yes | Image key (e.g., "thumbnail", "banner") |
| `file` | `bytes` | Yes | Image binary data |
| `extension` | `string` | Yes | File extension (e.g., "png", "jpg") |

**Response: `UpdateGameImageResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid image data or extension |
| `EXTERNAL_SERVICE_ERROR` (6000) | `UNAVAILABLE` | Image storage service failed |

---

#### AddTag / RemoveTag

Add or remove a tag from a game.

**Request: `AddGameTagCommand` / `RemoveGameTagCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `tag` | `string` | Yes | Tag name |

**Response: `AddGameTagResult` / `RemoveGameTagResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid tag name |
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | Tag already exists on game (AddTag only) |

---

#### GetFreespinPreset

Get available freespin preset configuration for a game.

**Request: `GetFreespinPresetQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |

**Response: `GetFreespinPresetResult`**

| Field | Type | Description |
|-------|------|-------------|
| `preset` | `map<string, string>` | Preset field names and their available values |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | Aggregator doesn't support freespins |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | Aggregator returned an error |

---

#### CreateFreespin

Create a freespin bonus for a player.

**Request: `CreateFreespinCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |
| `player_id` | `string` | Yes | Player identifier |
| `reference_id` | `string` | Yes | Unique reference ID (for idempotency) |
| `currency` | `string` | Yes | Currency code |
| `start_at` | `TimestampDto` | Yes | Freespin validity start |
| `end_at` | `TimestampDto` | Yes | Freespin validity end |
| `preset_values` | `map<string, int32>` | Yes | Selected preset values |

**Response: `CreateFreespinResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid request parameters |
| `INVALID_PRESET` (4003) | `INVALID_ARGUMENT` | Invalid preset configuration |
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | Freespin with reference_id already exists |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | Aggregator doesn't support freespins |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | Aggregator returned an error |

---

#### CancelFreespin

Cancel an existing freespin.

**Request: `CancelFreespinCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |
| `reference_id` | `string` | Yes | Freespin reference ID |

**Response: `CancelFreespinResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Game or freespin not found |
| `ILLEGAL_STATE` (1003) | `FAILED_PRECONDITION` | Freespin already used or cancelled |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | Aggregator doesn't support freespin cancellation |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | Aggregator returned an error |

---

### RoundService

Query betting rounds with aggregated amounts.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindRoundQuery` | `FindRoundResult` | Get a single round by ID |
| `FindAll` | `FindAllRoundQuery` | `FindAllRoundResult` | List rounds with filters |

---

#### Find

Get a single round by ID.

**Request: `FindRoundQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | Yes | Round UUID |

**Response: `FindRoundResult`**

| Field | Type | Description |
|-------|------|-------------|
| `item` | `RoundItemDto` | Round with aggregated data |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `ROUND_NOT_FOUND` (4002) | `NOT_FOUND` | Round with specified ID does not exist |

---

#### FindAll

List rounds with pagination and filters.

**Request: `FindAllRoundQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `game_identity` | `string` | No | Filter by game |
| `provider_identity` | `string` | No | Filter by provider |
| `player_id` | `string` | No | Filter by player |
| `free_spin_id` | `string` | No | Filter by freespin ID |
| `finished` | `bool` | No | Filter by completion status |
| `start_at` | `google.protobuf.Timestamp` | No | Filter: created_at >= start_at |
| `end_at` | `google.protobuf.Timestamp` | No | Filter: created_at <= end_at |
| `min_place_amount` | `int64` | No | Filter: total place amount >= value |
| `max_place_amount` | `int64` | No | Filter: total place amount <= value |
| `min_settle_amount` | `int64` | No | Filter: total settle amount >= value |
| `max_settle_amount` | `int64` | No | Filter: total settle amount <= value |

**Response: `FindAllRoundResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `RoundItemDto[]` | List of rounds |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `providers` | `ProviderDto[]` | Related providers |
| `games` | `GameDto[]` | Related games |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid pagination or date parameters |

---

### ProviderService

Manage game providers.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindProviderQuery` | `FindProviderResult` | Get a single provider |
| `FindAll` | `FindAllProviderQuery` | `FindAllProviderResult` | List providers |
| `Update` | `UpdateProviderCommand` | `UpdateProviderResult` | Update provider config |
| `UpdateImage` | `UpdateProviderImageCommand` | `UpdateProviderImageResult` | Upload provider image |

---

#### Find

Get a single provider with game counts.

**Request: `FindProviderQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |

**Response: `FindProviderResult`**

| Field | Type | Description |
|-------|------|-------------|
| `provider` | `ProviderDto` | Provider entity |
| `aggregator` | `AggregatorInfoDto` | Aggregator info |
| `active_games` | `int32` | Count of active games |
| `total_games` | `int32` | Total game count |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Provider not found |

---

#### FindAll

List providers with pagination and filters.

**Request: `FindAllProviderQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by name or identity |
| `active` | `bool` | No | Filter by active status |
| `aggregator_identity` | `string` | No | Filter by aggregator |

**Response: `FindAllProviderResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `ProviderItemDto[]` | List of providers |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `aggregators` | `AggregatorInfoDto[]` | Related aggregators |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid pagination parameters |

---

#### Update

Update provider configuration.

**Request: `UpdateProviderCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |
| `active` | `bool` | No | Enable/disable provider |
| `order` | `int32` | No | Display order |
| `aggregator_identity` | `string` | No | Assign to aggregator |

**Response: `UpdateProviderResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Provider or aggregator not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid field values |

---

#### UpdateImage

Upload or update a provider image.

**Request: `UpdateProviderImageCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |
| `key` | `string` | Yes | Image key (e.g., "logo") |
| `file` | `bytes` | Yes | Image binary data |
| `extension` | `string` | Yes | File extension |

**Response: `UpdateProviderImageResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Provider not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid image data or extension |
| `EXTERNAL_SERVICE_ERROR` (6000) | `UNAVAILABLE` | Image storage service failed |

---

### CollectionService

Manage game collections/categories.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Create` | `CreateCollectionCommand` | `CreateCollectionResult` | Create a new collection |
| `Find` | `FindCollectionQuery` | `FindCollectionResult` | Get a single collection |
| `FindAll` | `FindAllCollectionQuery` | `FindAllCollectionResult` | List collections |
| `Update` | `UpdateCollectionCommand` | `UpdateCollectionResult` | Update collection config |
| `UpdateGames` | `UpdateCollectionGamesCommand` | `UpdateCollectionGamesResult` | Add/remove games |

---

#### Create

Create a new collection.

**Request: `CreateCollectionCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Unique collection identifier |
| `name` | `LocaleNameDto` | Yes | Localized names |
| `active` | `bool` | No | Active status (default: true) |
| `order` | `int32` | No | Display order (default: 100) |

**Response: `CreateCollectionResult`**

| Field | Type | Description |
|-------|------|-------------|
| `collection` | `CollectionDto` | The created collection |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | Collection with this identity already exists |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid identity or name |

---

#### Find

Get a single collection with counts.

**Request: `FindCollectionQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |

**Response: `FindCollectionResult`**

| Field | Type | Description |
|-------|------|-------------|
| `collection` | `CollectionDto` | Collection entity |
| `provider_count` | `int32` | Number of unique providers |
| `game_count` | `int32` | Number of games |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Collection not found |

---

#### FindAll

List collections with pagination and filters.

**Request: `FindAllCollectionQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by identity |
| `active` | `bool` | No | Filter by active status |

**Response: `FindAllCollectionResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `CollectionItemDto[]` | List of collections |
| `pagination` | `PaginationMetaDto` | Pagination metadata |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid pagination parameters |

---

#### Update

Update collection configuration.

**Request: `UpdateCollectionCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |
| `active` | `bool` | No | Enable/disable collection |
| `order` | `int32` | No | Display order |

**Response: `UpdateCollectionResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Collection not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid field values |

---

#### UpdateGames

Add or remove games from a collection.

**Request: `UpdateCollectionGamesCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |
| `add_games` | `string[]` | No | Game identities to add |
| `remove_games` | `string[]` | No | Game identities to remove |

**Response: `UpdateCollectionGamesResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Collection or game not found |
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | Game already in collection |

---

### AggregatorService

Manage aggregator configurations.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Create` | `CreateAggregatorCommand` | `CreateAggregatorResult` | Create new aggregator |
| `Find` | `FindAggregatorQuery` | `FindAggregatorResult` | Get a single aggregator |
| `FindAll` | `FindAllAggregatorQuery` | `FindAllAggregatorResult` | List aggregators |
| `Update` | `UpdateAggregatorCommand` | `UpdateAggregatorResult` | Update aggregator config |

---

#### Create

Create a new aggregator configuration.

**Request: `CreateAggregatorCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Unique identifier |
| `aggregator` | `AggregatorTypeDto` | Yes | Aggregator type |
| `config` | `map<string, string>` | Yes | Configuration key-value pairs |
| `active` | `bool` | No | Initial active status |

**Response: `CreateAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `aggregator` | `AggregatorInfoDto` | Created aggregator |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | Aggregator with this identity exists |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid configuration or missing required fields |

---

#### Find

Get a single aggregator by identity.

**Request: `FindAggregatorQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Aggregator identifier |

**Response: `FindAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `aggregator` | `AggregatorInfoDto` | Aggregator details |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Aggregator not found |

---

#### FindAll

List aggregators with pagination and filters.

**Request: `FindAllAggregatorQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by identity |
| `active` | `bool` | No | Filter by active status |

**Response: `FindAllAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `AggregatorInfoDto[]` | List of aggregators |
| `pagination` | `PaginationMetaDto` | Pagination metadata |

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid pagination parameters |

---

#### Update

Update aggregator configuration.

**Request: `UpdateAggregatorCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Aggregator identifier |
| `active` | `bool` | No | Enable/disable aggregator |
| `config` | `map<string, string>` | No | Configuration to update |

**Response: `UpdateAggregatorResult`**

Empty response. Success indicated by no error.

**Errors:**

| Error Code | gRPC Status | Condition |
|------------|-------------|-----------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | Aggregator not found |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | Invalid configuration |

---

## Enums

### PlatformDto

Player platform type.

| Name | Value | Description |
|------|-------|-------------|
| `PLATFORM_UNSPECIFIED` | 0 | Not specified |
| `PLATFORM_DESKTOP` | 1 | Desktop browser |
| `PLATFORM_MOBILE` | 2 | Mobile device |
| `PLATFORM_DOWNLOAD` | 3 | Downloadable client |

### AggregatorTypeDto

Supported game aggregators.

| Name | Value | Description |
|------|-------|-------------|
| `AGGREGATOR_UNSPECIFIED` | 0 | Not specified |
| `AGGREGATOR_ONEGAMEHUB` | 1 | OneGameHub |
| `AGGREGATOR_PRAGMATIC` | 2 | Pragmatic Play |
| `AGGREGATOR_PATEPLAY` | 3 | Pateplay |

### SpinTypeDto

Type of spin/bet transaction.

| Name | Value | Description |
|------|-------|-------------|
| `SPIN_TYPE_UNSPECIFIED` | 0 | Not specified |
| `SPIN_TYPE_PLACE` | 1 | Bet placement |
| `SPIN_TYPE_SETTLE` | 2 | Win settlement |
| `SPIN_TYPE_ROLLBACK` | 3 | Bet rollback/refund |

### RoundStatusDto

Round completion status.

| Name | Value | Description |
|------|-------|-------------|
| `ROUND_STATUS_UNSPECIFIED` | 0 | Not specified |
| `ROUND_STATUS_ACTIVE` | 1 | Round in progress |
| `ROUND_STATUS_FINISHED` | 2 | Round completed |

---

## Common DTOs

### PaginationRequestDto

Pagination request parameters.

| Field | Type | Description |
|-------|------|-------------|
| `page` | `int32` | Page number (0-indexed) |
| `size` | `int32` | Items per page |

### PaginationMetaDto

Pagination response metadata.

| Field | Type | Description |
|-------|------|-------------|
| `page` | `int32` | Current page number |
| `size` | `int32` | Items per page |
| `total_elements` | `int64` | Total number of items |
| `total_pages` | `int32` | Total number of pages |

### TimestampDto

Timestamp representation.

| Field | Type | Description |
|-------|------|-------------|
| `seconds` | `int64` | Seconds since Unix epoch |
| `nanos` | `int32` | Nanoseconds (0-999,999,999) |

### LocaleNameDto

Localized name map.

| Field | Type | Description |
|-------|------|-------------|
| `values` | `map<string, string>` | Locale code to name (e.g., `{"en": "Slots", "de": "Spielautomaten"}`) |

### ImageMapDto

Image URL map.

| Field | Type | Description |
|-------|------|-------------|
| `images` | `map<string, string>` | Image key to URL (e.g., `{"thumbnail": "https://..."}`) |

### GameDto

Core game entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `string` | Game name |
| `provider_identity` | `string` | Provider reference |
| `images` | `ImageMapDto` | Game images |
| `bonus_bet_enable` | `bool` | Bonus bet support |
| `bonus_wagering_enable` | `bool` | Bonus wagering support |
| `tags` | `string[]` | Game tags |
| `active` | `bool` | Active status |

### GameVariantDto

Game variant for a specific aggregator.

| Field | Type | Description |
|-------|------|-------------|
| `symbol` | `string` | Game symbol/code |
| `game_identity` | `string` | Game reference (optional) |
| `name` | `string` | Variant name |
| `provider_name` | `string` | Provider name from aggregator |
| `aggregator` | `AggregatorTypeDto` | Aggregator type |
| `free_spin_enable` | `bool` | Freespin support |
| `free_chip_enable` | `bool` | Free chip support |
| `jackpot_enable` | `bool` | Jackpot support |
| `demo_enable` | `bool` | Demo mode support |
| `bonus_buy_enable` | `bool` | Bonus buy feature |
| `locales` | `string[]` | Supported locales |
| `platforms` | `PlatformDto[]` | Supported platforms |
| `play_lines` | `int32` | Number of play lines |

### GameItemDto

Game with variant and collection references.

| Field | Type | Description |
|-------|------|-------------|
| `game` | `GameDto` | Game entity |
| `active_variant` | `GameVariantDto` | Active variant |
| `collection_identities` | `string[]` | Collection references |

### ProviderDto

Provider entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `string` | Provider name |
| `images` | `ImageMapDto` | Provider images |
| `order` | `int32` | Display order |
| `aggregator_identity` | `string` | Aggregator reference (optional) |
| `active` | `bool` | Active status |

### ProviderItemDto

Provider with game statistics.

| Field | Type | Description |
|-------|------|-------------|
| `provider` | `ProviderDto` | Provider entity |
| `aggregator_identity` | `string` | Aggregator reference |
| `active_games` | `int32` | Active game count |
| `total_games` | `int32` | Total game count |

### CollectionDto

Collection/category entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `LocaleNameDto` | Localized names |
| `active` | `bool` | Active status |
| `order` | `int32` | Display order |

### CollectionItemDto

Collection with statistics.

| Field | Type | Description |
|-------|------|-------------|
| `collection` | `CollectionDto` | Collection entity |
| `provider_count` | `int32` | Unique provider count |
| `game_count` | `int32` | Game count |

### AggregatorInfoDto

Aggregator configuration.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `config` | `map<string, string>` | Configuration key-values |
| `aggregator` | `AggregatorTypeDto` | Aggregator type |
| `active` | `bool` | Active status |

### RoundDto

Round entity.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | UUID |
| `session_id` | `string` | Session UUID |
| `game_id` | `string` | Game UUID |
| `ext_id` | `string` | External round ID |
| `finished` | `bool` | Completion status |
| `created_at` | `TimestampDto` | Creation timestamp |
| `finished_at` | `TimestampDto` | Finish timestamp (optional) |

### RoundItemDto

Round with aggregated amounts.

| Field | Type | Description |
|-------|------|-------------|
| `round` | `RoundDto` | Round entity |
| `provider_identity` | `string` | Provider identifier |
| `game_identity` | `string` | Game identifier |
| `player_id` | `string` | Player identifier |
| `currency` | `string` | Currency code |
| `total_place_real` | `int64` | Total real money placed (minor units) |
| `total_place_bonus` | `int64` | Total bonus money placed |
| `total_settle_real` | `int64` | Total real money won |
| `total_settle_bonus` | `int64` | Total bonus money won |

### SpinDto

Spin/transaction entity.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | UUID |
| `round_id` | `string` | Round UUID |
| `type` | `SpinTypeDto` | Transaction type |
| `amount` | `string` | Total amount (BigInteger string) |
| `real_amount` | `string` | Real money amount |
| `bonus_amount` | `string` | Bonus amount |
| `ext_id` | `string` | External transaction ID |
| `reference_id` | `string` | Reference ID (optional) |
| `free_spin_id` | `string` | Freespin ID (optional) |

---

## Error Handling

All errors are returned as gRPC `StatusException` with structured metadata. The system uses domain-specific error codes mapped to appropriate gRPC status codes, providing rich error context through metadata headers.

### Error Codes

Error codes are organized into ranges by category:

| Range | Category | Description |
|-------|----------|-------------|
| 1xxx | General | Common errors like not found, validation, duplicates |
| 2xxx | Balance/Betting | Financial operation errors |
| 3xxx | Session | Session and authentication errors |
| 4xxx | Game | Game and round specific errors |
| 5xxx | Aggregator | Aggregator integration errors |
| 6xxx | External | External service errors |
| 9xxx | Internal | Internal server errors |

**Complete Error Code List:**

| Code | Name | Description |
|------|------|-------------|
| 1000 | `NOT_FOUND` | Entity not found |
| 1001 | `VALIDATION_ERROR` | Validation failed |
| 1002 | `DUPLICATE_ENTITY` | Entity already exists |
| 1003 | `ILLEGAL_STATE` | Operation not allowed in current state |
| 2000 | `INSUFFICIENT_BALANCE` | Insufficient funds |
| 2001 | `SPIN_LIMIT_EXCEEDED` | Spin amount exceeds limit |
| 3000 | `SESSION_INVALID` | Session expired/invalid |
| 4000 | `GAME_UNAVAILABLE` | Game not available |
| 4001 | `ROUND_FINISHED` | Round already completed |
| 4002 | `ROUND_NOT_FOUND` | Round not found |
| 4003 | `INVALID_PRESET` | Invalid freespin preset |
| 5000 | `AGGREGATOR_NOT_SUPPORTED` | Aggregator not supported |
| 5001 | `AGGREGATOR_ERROR` | Aggregator returned error |
| 6000 | `EXTERNAL_SERVICE_ERROR` | External service failed |
| 9999 | `INTERNAL_ERROR` | Internal server error |

### gRPC Status Mapping

Each domain error code maps to a specific gRPC status code:

| Error Code | gRPC Status | HTTP Equivalent |
|------------|-------------|-----------------|
| `NOT_FOUND` (1000) | `NOT_FOUND` | 404 |
| `ROUND_NOT_FOUND` (4002) | `NOT_FOUND` | 404 |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` | 400 |
| `INVALID_PRESET` (4003) | `INVALID_ARGUMENT` | 400 |
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` | 409 |
| `INSUFFICIENT_BALANCE` (2000) | `FAILED_PRECONDITION` | 412 |
| `SPIN_LIMIT_EXCEEDED` (2001) | `FAILED_PRECONDITION` | 412 |
| `ROUND_FINISHED` (4001) | `FAILED_PRECONDITION` | 412 |
| `ILLEGAL_STATE` (1003) | `FAILED_PRECONDITION` | 412 |
| `SESSION_INVALID` (3000) | `UNAUTHENTICATED` | 401 |
| `GAME_UNAVAILABLE` (4000) | `UNAVAILABLE` | 503 |
| `EXTERNAL_SERVICE_ERROR` (6000) | `UNAVAILABLE` | 503 |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` | 501 |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` | 500 |
| `INTERNAL_ERROR` (9999) | `INTERNAL` | 500 |

### Error Metadata

All errors include metadata headers in gRPC trailers. These provide structured error details for programmatic handling.

**Common Headers (present on all errors):**

| Header | Type | Description |
|--------|------|-------------|
| `x-error-code` | string | Error code name (e.g., `NOT_FOUND`) |
| `x-error-code-value` | string | Numeric code (e.g., `1000`) |

**Error-Specific Headers:**

| Header | Used By | Description |
|--------|---------|-------------|
| `x-entity-type` | `NOT_FOUND`, `DUPLICATE_ENTITY` | Entity type (e.g., "Game", "Provider") |
| `x-identifier` | `NOT_FOUND`, `ROUND_NOT_FOUND`, `SESSION_INVALID`, `GAME_UNAVAILABLE`, `ROUND_FINISHED`, `INVALID_PRESET`, `AGGREGATOR_NOT_SUPPORTED` | Entity identifier |
| `x-field` | `VALIDATION_ERROR`, `ILLEGAL_STATE` | Field or operation name |
| `x-reason` | `VALIDATION_ERROR`, `SESSION_INVALID`, `GAME_UNAVAILABLE`, `INVALID_PRESET`, `EXTERNAL_SERVICE_ERROR`, `ILLEGAL_STATE` | Additional context |
| `x-player-id` | `INSUFFICIENT_BALANCE`, `SPIN_LIMIT_EXCEEDED` | Player identifier |
| `x-required-amount` | `INSUFFICIENT_BALANCE` | Amount required (in minor units) |
| `x-available-amount` | `INSUFFICIENT_BALANCE` | Available balance (in minor units) |
| `x-spin-amount` | `SPIN_LIMIT_EXCEEDED` | Attempted spin amount |
| `x-limit` | `SPIN_LIMIT_EXCEEDED` | Configured spin limit |
| `x-service` | `EXTERNAL_SERVICE_ERROR` | External service name |

### Error Types Reference

Detailed breakdown of each error type with its metadata:

#### NOT_FOUND (1000)

Entity was not found in the system.

```
gRPC Status: NOT_FOUND
Metadata:
  x-error-code: NOT_FOUND
  x-error-code-value: 1000
  x-entity-type: <entity type>  # e.g., "Game", "Provider", "Collection"
  x-identifier: <identifier>     # The identifier that was not found
```

#### ROUND_NOT_FOUND (4002)

Round was not found. Separate error code for round-specific queries.

```
gRPC Status: NOT_FOUND
Metadata:
  x-error-code: ROUND_NOT_FOUND
  x-error-code-value: 4002
  x-entity-type: Round
  x-identifier: <round_id>
```

#### VALIDATION_ERROR (1001)

Input validation failed.

```
gRPC Status: INVALID_ARGUMENT
Metadata:
  x-error-code: VALIDATION_ERROR
  x-error-code-value: 1001
  x-field: <field name>
  x-reason: <validation failure reason>
```

#### DUPLICATE_ENTITY (1002)

Entity already exists.

```
gRPC Status: ALREADY_EXISTS
Metadata:
  x-error-code: DUPLICATE_ENTITY
  x-error-code-value: 1002
  x-entity-type: <entity type>
  x-identifier: <identifier>
```

#### ILLEGAL_STATE (1003)

Operation not allowed in current state.

```
gRPC Status: FAILED_PRECONDITION
Metadata:
  x-error-code: ILLEGAL_STATE
  x-error-code-value: 1003
  x-field: <operation name>
  x-reason: <current state>
```

#### INSUFFICIENT_BALANCE (2000)

Player has insufficient balance.

```
gRPC Status: FAILED_PRECONDITION
Metadata:
  x-error-code: INSUFFICIENT_BALANCE
  x-error-code-value: 2000
  x-player-id: <player_id>
  x-required-amount: <required amount in minor units>
  x-available-amount: <available balance in minor units>
```

#### SPIN_LIMIT_EXCEEDED (2001)

Spin amount exceeds configured limit.

```
gRPC Status: FAILED_PRECONDITION
Metadata:
  x-error-code: SPIN_LIMIT_EXCEEDED
  x-error-code-value: 2001
  x-player-id: <player_id>
  x-spin-amount: <attempted spin amount>
  x-limit: <configured spin limit>
```

#### SESSION_INVALID (3000)

Session is invalid or expired.

```
gRPC Status: UNAUTHENTICATED
Metadata:
  x-error-code: SESSION_INVALID
  x-error-code-value: 3000
  x-identifier: <session token>
  x-reason: <reason>
```

#### GAME_UNAVAILABLE (4000)

Game is not available for play.

```
gRPC Status: UNAVAILABLE
Metadata:
  x-error-code: GAME_UNAVAILABLE
  x-error-code-value: 4000
  x-identifier: <game_identity>
  x-reason: <reason>
```

#### ROUND_FINISHED (4001)

Round has already been completed.

```
gRPC Status: FAILED_PRECONDITION
Metadata:
  x-error-code: ROUND_FINISHED
  x-error-code-value: 4001
  x-identifier: <round_id>
```

#### INVALID_PRESET (4003)

Invalid freespin preset configuration.

```
gRPC Status: INVALID_ARGUMENT
Metadata:
  x-error-code: INVALID_PRESET
  x-error-code-value: 4003
  x-identifier: <preset_id>
  x-reason: <reason>
```

#### AGGREGATOR_NOT_SUPPORTED (5000)

Aggregator is not supported for the operation.

```
gRPC Status: UNIMPLEMENTED
Metadata:
  x-error-code: AGGREGATOR_NOT_SUPPORTED
  x-error-code-value: 5000
  x-identifier: <aggregator>
```

#### AGGREGATOR_ERROR (5001)

Aggregator returned an error.

```
gRPC Status: INTERNAL
Metadata:
  x-error-code: AGGREGATOR_ERROR
  x-error-code-value: 5001
```

#### EXTERNAL_SERVICE_ERROR (6000)

External service (wallet, storage, etc.) failed.

```
gRPC Status: UNAVAILABLE
Metadata:
  x-error-code: EXTERNAL_SERVICE_ERROR
  x-error-code-value: 6000
  x-service: <service name>
  x-reason: <error details>
```

#### INTERNAL_ERROR (9999)

Internal server error.

```
gRPC Status: INTERNAL
Metadata:
  x-error-code: INTERNAL_ERROR
  x-error-code-value: 9999
```

### Client Examples

#### Go

```go
import (
    "google.golang.org/grpc/status"
    "google.golang.org/grpc/codes"
)

response, err := client.Find(ctx, request)
if err != nil {
    st := status.Convert(err)

    switch st.Code() {
    case codes.NotFound:
        // Handle not found
        md := st.Trailer()
        errorCode := md.Get("x-error-code")
        identifier := md.Get("x-identifier")
        entityType := md.Get("x-entity-type")

    case codes.FailedPrecondition:
        // Handle precondition failures (balance, limits, etc.)
        md := st.Trailer()
        errorCode := md.Get("x-error-code")
        if errorCode[0] == "INSUFFICIENT_BALANCE" {
            required := md.Get("x-required-amount")
            available := md.Get("x-available-amount")
        }

    case codes.InvalidArgument:
        // Handle validation errors
        md := st.Trailer()
        field := md.Get("x-field")
        reason := md.Get("x-reason")
    }
}
```

#### Python

```python
import grpc

try:
    response = stub.Find(request)
except grpc.RpcError as e:
    if e.code() == grpc.StatusCode.NOT_FOUND:
        metadata = dict(e.trailing_metadata())
        error_code = metadata.get('x-error-code')
        identifier = metadata.get('x-identifier')
        entity_type = metadata.get('x-entity-type')

    elif e.code() == grpc.StatusCode.FAILED_PRECONDITION:
        metadata = dict(e.trailing_metadata())
        error_code = metadata.get('x-error-code')
        if error_code == 'INSUFFICIENT_BALANCE':
            required = metadata.get('x-required-amount')
            available = metadata.get('x-available-amount')

    elif e.code() == grpc.StatusCode.INVALID_ARGUMENT:
        metadata = dict(e.trailing_metadata())
        field = metadata.get('x-field')
        reason = metadata.get('x-reason')
```

#### Node.js

```javascript
const grpc = require('@grpc/grpc-js');

client.Find(request, (err, response) => {
    if (err) {
        const metadata = err.metadata.getMap();
        const errorCode = metadata['x-error-code'];
        const errorCodeValue = parseInt(metadata['x-error-code-value']);

        switch (err.code) {
            case grpc.status.NOT_FOUND:
                const identifier = metadata['x-identifier'];
                const entityType = metadata['x-entity-type'];
                break;

            case grpc.status.FAILED_PRECONDITION:
                if (errorCode === 'INSUFFICIENT_BALANCE') {
                    const required = metadata['x-required-amount'];
                    const available = metadata['x-available-amount'];
                }
                break;

            case grpc.status.INVALID_ARGUMENT:
                const field = metadata['x-field'];
                const reason = metadata['x-reason'];
                break;
        }
    }
});
```

#### Java/Kotlin

```kotlin
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.Metadata

try {
    val response = stub.find(request)
} catch (e: StatusException) {
    val metadata = Status.trailersFromThrowable(e)
    val errorCode = metadata?.get(
        Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)
    )
    val errorCodeValue = metadata?.get(
        Metadata.Key.of("x-error-code-value", Metadata.ASCII_STRING_MARSHALLER)
    )?.toIntOrNull()

    when (e.status.code) {
        Status.Code.NOT_FOUND -> {
            val identifier = metadata?.get(
                Metadata.Key.of("x-identifier", Metadata.ASCII_STRING_MARSHALLER)
            )
            val entityType = metadata?.get(
                Metadata.Key.of("x-entity-type", Metadata.ASCII_STRING_MARSHALLER)
            )
        }
        Status.Code.FAILED_PRECONDITION -> {
            when (errorCode) {
                "INSUFFICIENT_BALANCE" -> {
                    val required = metadata?.get(
                        Metadata.Key.of("x-required-amount", Metadata.ASCII_STRING_MARSHALLER)
                    )
                    val available = metadata?.get(
                        Metadata.Key.of("x-available-amount", Metadata.ASCII_STRING_MARSHALLER)
                    )
                }
            }
        }
        Status.Code.INVALID_ARGUMENT -> {
            val field = metadata?.get(
                Metadata.Key.of("x-field", Metadata.ASCII_STRING_MARSHALLER)
            )
            val reason = metadata?.get(
                Metadata.Key.of("x-reason", Metadata.ASCII_STRING_MARSHALLER)
            )
        }
    }
}
```

---

## Connection

**Default Port:** `5050`

### Client Configuration Recommendations

- **Timeout:** Set deadlines (5-30 seconds depending on operation)
- **Keep-alive:** Enable for long-running connections
- **Max message size:** 50 MB recommended for image uploads
- **TLS:** Required for production environments
