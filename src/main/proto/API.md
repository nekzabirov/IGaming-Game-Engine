# Casino Engine gRPC API

Package: `game.v1` | Java package: `com.nekgamebling.game.v1`

---

## GameService

Game catalog management, launching, and player favorites.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `SaveGameCommand` | `Empty` | Create or update a game |
| `Find` | `FindGameQuery` | `FindGameQuery.Result` | Get a single game by identity |
| `FindAll` | `FindAllGameQuery` | `FindAllGameQuery.Result` | List/filter games with pagination |
| `Batch` | `BatchGameQuery` | `BatchGameQuery.Result` | Batch fetch games |
| `UpdateImage` | `UpdateGameImageCommand` | `Empty` | Upload/replace a game image |
| `Play` | `PlayGameCommand` | `PlayGameCommand.Result` | Open a real-money game session |
| `OpenDemo` | `OpenDemoQuery` | `OpenDemoQuery.Result` | Open a demo game session |
| `AddFavourite` | `GameFavouriteCommand` | `Empty` | Add game to player favorites |
| `RemoveFavourite` | `GameFavouriteCommand` | `Empty` | Remove game from player favorites |

### Save

Create or update a game's editable properties.

```protobuf
message SaveGameCommand {
  string identity = 1;              // Game unique identifier
  string name = 2;                  // Display name
  bool bonus_bet_enable = 3;        // Allow bonus bets
  bool bonus_wagering_enable = 4;   // Allow bonus wagering
  repeated string tags = 5;         // Searchable tags
  string provider_identity = 6;     // Parent provider identity
}
```

### Find

Returns a game with its provider, aggregator, and collections.

```protobuf
// Request
message FindGameQuery {
  string identity = 1;
}

// Response
message FindGameQuery.Result {
  GameDto item = 1;
  ProviderDto provider = 2;
  AggregatorDto aggregator = 3;
  repeated CollectionDto collections = 4;
}
```

### FindAll

Paginated game listing with filters. Returns games with providers, aggregators, and collections used across the result set.

```protobuf
// Request
message FindAllGameQuery {
  string query = 1;                              // Free-text search
  optional bool active = 2;                      // Filter by active status
  repeated string provider_identities = 3;       // Filter by providers
  repeated string collection_identities = 4;     // Filter by collections
  repeated string tags = 5;                      // Filter by tags
  optional bool bonus_bet_enable = 6;
  optional bool bonus_wagering_enable = 7;
  optional bool free_spin_enable = 8;
  optional bool free_chip_enable = 9;
  optional bool jackpot_enable = 10;
  optional bool demo_enable = 11;
  optional bool bonus_buy_enable = 12;
  int32 page_num = 13;                           // Page number (0-based)
  int32 page_size = 14;                          // Items per page
}

// Response
message FindAllGameQuery.Result {
  repeated Item items = 1;                       // Game + provider pairs
  repeated ProviderDto providers = 2;            // All referenced providers
  repeated AggregatorDto aggregators = 3;        // All referenced aggregators
  repeated CollectionDto collections = 4;        // All referenced collections
  int32 total_items = 5;                         // Total count for pagination

  message Item {
    GameDto game = 1;
    ProviderDto provider = 2;
  }
}
```

### Batch

Fetch games by identities. Response structure matches `FindAll`.

```protobuf
// Request
message BatchGameQuery {
  repeated string identities = 1;  // game identities to fetch
}

// Response
message BatchGameQuery.Result {
  repeated Item items = 1;
  repeated ProviderDto providers = 2;
  repeated AggregatorDto aggregators = 3;
  repeated CollectionDto collections = 4;

  message Item {
    GameDto game = 1;
    ProviderDto provider = 2;
  }
}
```

### UpdateImage

Upload or replace a game image by key (e.g. `"thumbnail"`, `"banner"`).

```protobuf
message UpdateGameImageCommand {
  string identity = 1;     // Game identity
  string key = 2;          // Image key (e.g. "thumbnail")
  bytes file = 3;          // Raw image bytes
  string extension = 4;    // File extension (e.g. "png", "jpg")
}
```

### Play

Open a real-money game session. Returns a launch URL the player should be redirected to.

```protobuf
// Request
message PlayGameCommand {
  string identity = 1;                      // Game identity
  string player_id = 2;                     // Player UUID
  string locale = 3;                        // Player locale (e.g. "en")
  PlatformDto platform = 4;                 // DESKTOP / MOBILE / DOWNLOAD
  string currency = 5;                      // Currency code (e.g. "USD")
  optional int64 max_spin_place_amount = 6; // Max bet limit (minor units)
}

// Response
message PlayGameCommand.Result {
  string launch_url = 1;
}
```

### OpenDemo

Open a demo (free-play) game session. No player authentication required.

```protobuf
// Request
message OpenDemoQuery {
  string identity = 1;       // Game identity
  string currency = 2;       // Currency code
  string locale = 3;         // Locale
  PlatformDto platform = 4;  // Platform
  string lobby_url = 5;      // URL to redirect on exit
}

// Response
message OpenDemoQuery.Result {
  string launch_url = 1;
}
```

### AddFavourite / RemoveFavourite

Add or remove a game from a player's favorites list.

```protobuf
message GameFavouriteCommand {
  string identity = 1;   // Game identity
  string player_id = 2;  // Player UUID
}
```

---

## ProviderService

Game provider management (e.g. "Pragmatic Play", "NetEnt").

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `ProviderDto` | `Empty` | Create or update a provider |
| `Find` | `FindProviderQuery` | `FindProviderQuery.Result` | Get provider with game counts |
| `FindAll` | `FindAllProviderQuery` | `FindAllProviderQuery.Result` | List/filter providers with pagination |
| `Batch` | `BatchProviderQuery` | `BatchProviderQuery.Result` | Batch fetch providers by identities |
| `UpdateImage` | `UpdateProviderImageCommand` | `Empty` | Upload/replace a provider image |

### Save

Create or update a provider. Pass the full `ProviderDto`.

```protobuf
message ProviderDto {
  string identity = 1;              // Provider unique identifier
  string name = 2;                  // Display name
  map<string, string> images = 3;   // Key → CDN URL
  int32 order = 4;                  // Sort order
  bool active = 5;                  // Active status
  string aggregator_identity = 6;   // Parent aggregator identity
}
```

### Find

Returns a provider with its aggregator and game counts.

```protobuf
// Request
message FindProviderQuery {
  string identity = 1;
}

// Response
message FindProviderQuery.Result {
  ProviderDto item = 1;
  AggregatorDto aggregator = 2;
  int32 active_game_count = 3;       // Number of active games
  int32 deactivate_game_count = 4;   // Number of inactive games
}
```

### FindAll

Paginated provider listing with optional filters.

```protobuf
// Request
message FindAllProviderQuery {
  string query = 1;                          // Free-text search
  optional bool active = 2;                  // Filter by status
  optional string aggregator_identity = 3;   // Filter by aggregator
  int32 page_num = 4;
  int32 page_size = 5;
  repeated string tags = 6;                  // Filter by game tags (providers having games with these tags)
  repeated string collection_identities = 7; // Filter by collection identities (providers having games in these collections)
}

// Response
message FindAllProviderQuery.Result {
  repeated Item items = 1;
  repeated AggregatorDto aggregators = 2;
  int32 total_items = 3;

  message Item {
    ProviderDto provider = 1;
    int32 active_game_count = 3;
    int32 deactivate_game_count = 4;
  }
}
```

### Batch

Fetch providers by identities. Returns providers with their aggregators.

```protobuf
// Request
message BatchProviderQuery {
  repeated string identities = 1;  // provider identities to fetch
}

// Response
message BatchProviderQuery.Result {
  repeated Item items = 1;
  repeated AggregatorDto aggregators = 2;

  message Item {
    ProviderDto provider = 1;
  }
}
```

### UpdateImage

```protobuf
message UpdateProviderImageCommand {
  string identity = 1;
  string key = 2;
  bytes file = 3;
  string extension = 4;
}
```

---

## AggregatorService

Aggregator configuration management (e.g. ONEGAMEHUB, PRAGMATIC, PATEPLAY).

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `AggregatorDto` | `Empty` | Create or update an aggregator |
| `Find` | `FindAggregatorQuery` | `AggregatorDto` | Get aggregator by identity |
| `FindAll` | `FindAllAggregatorQuery` | `FindAllAggregatorResult` | List/filter aggregators with pagination |

### Save

Create or update an aggregator. The `config` field is a JSON object with aggregator-specific settings (API keys, endpoints, etc.).

```protobuf
message AggregatorDto {
  string identity = 1;               // Aggregator unique identifier
  string integration = 2;            // Integration type: "ONEGAMEHUB", "PRAGMATIC", "PATEPLAY"
  google.protobuf.Struct config = 3; // Aggregator-specific JSON configuration
  bool active = 4;                   // Active status
}
```

### Find

```protobuf
message FindAggregatorQuery {
  string identity = 1;
}
// Returns: AggregatorDto
```

### FindAll

```protobuf
// Request
message FindAllAggregatorQuery {
  string query = 1;                  // Free-text search
  optional bool active = 2;         // Filter by status
  optional string integration = 3;  // Filter by integration type
  int32 page_num = 4;
  int32 page_size = 5;
}

// Response
message FindAllAggregatorResult {
  repeated AggregatorDto items = 1;
  int32 total_items = 2;
}
```

---

## CollectionService

Game collection management (e.g. "Hot Games", "New Releases") with multi-language support.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `CollectionDto` | `Empty` | Create or update a collection |
| `Find` | `FindCollectionQuery` | `FindCollectionQuery.Result` | Get collection with game/provider counts |
| `FindAll` | `FindAllCollectionQuery` | `FindAllCollectionQuery.Result` | List/filter collections with pagination |
| `Batch` | `BatchCollectionQuery` | `BatchCollectionQuery.Result` | Batch fetch collections by identities |
| `UpdateGames` | `UpdateCollectionGamesCommand` | `Empty` | Add/remove games from a collection |
| `UpdateImage` | `UpdateCollectionImageCommand` | `Empty` | Upload/replace a collection image |

### Save

```protobuf
message CollectionDto {
  string identity = 1;              // Collection unique identifier
  map<string, string> name = 2;     // Locale → name (e.g. {"en": "Hot Games", "de": "Heisse Spiele"})
  map<string, string> images = 3;   // Key → CDN URL
  bool active = 4;
  int32 order = 5;                  // Sort order
}
```

### Find

```protobuf
// Request
message FindCollectionQuery {
  string identity = 1;
}

// Response
message FindCollectionQuery.Result {
  CollectionDto item = 1;
  int32 game_active_count = 2;
  int32 game_deactivate_count = 3;
  int32 provider_count = 4;
}
```

### FindAll

```protobuf
// Request
message FindAllCollectionQuery {
  string query = 1;
  optional bool active = 2;
  int32 page_num = 3;
  int32 page_size = 4;
  repeated string tags = 5;            // Filter by game tags (collections containing games with these tags)
  repeated string provider_identities = 6;  // Filter by provider identities (collections containing games from these providers)
}

// Response
message FindAllCollectionQuery.Result {
  repeated Item items = 1;
  int32 total_items = 2;

  message Item {
    CollectionDto collection = 1;
    int32 game_active_count = 2;
    int32 game_deactivate_count = 3;
    int32 provider_count = 4;
  }
}
```

### Batch

Fetch collections by identities.

```protobuf
// Request
message BatchCollectionQuery {
  repeated string identities = 1;  // collection identities to fetch
}

// Response
message BatchCollectionQuery.Result {
  repeated Item items = 1;

  message Item {
    CollectionDto collection = 1;
  }
}
```

### UpdateGames

Add and/or remove games from a collection in a single call.

```protobuf
message UpdateCollectionGamesCommand {
  string identity = 1;             // Collection identity
  repeated string add_games = 2;   // Game identities to add
  repeated string remove_games = 3; // Game identities to remove
}
```

### UpdateImage

```protobuf
message UpdateCollectionImageCommand {
  string identity = 1;
  string key = 2;
  bytes file = 3;
  string extension = 4;
}
```

---

## FreespinService

Manage free spin campaigns for players on specific games.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `GetPreset` | `GetFreespinPresetQuery` | `GetFreespinPresetQuery.Result` | Get available freespin presets for a game |
| `Create` | `CreateFreespinCommand` | `Empty` | Issue freespins to a player |
| `Cancel` | `CancelFreespinCommand` | `Empty` | Cancel an active freespin campaign |

### GetPreset

Retrieve the available freespin configuration presets for a game (bet levels, coin values, etc.). The response is aggregator-specific JSON.

```protobuf
// Request
message GetFreespinPresetQuery {
  string game_identity = 1;
}

// Response
message GetFreespinPresetQuery.Result {
  google.protobuf.Struct preset = 1;  // Aggregator-specific preset JSON
}
```

### Create

Issue freespins to a player for a specific game.

```protobuf
message CreateFreespinCommand {
  string game_identity = 1;                  // Target game
  string player_id = 2;                      // Player UUID
  string reference_id = 3;                   // External reference (e.g. bonus ID)
  string currency = 4;                       // Currency code
  string start_at = 5;                       // Start date (ISO 8601)
  string end_at = 6;                         // Expiry date (ISO 8601)
  google.protobuf.Struct preset_values = 7;  // Preset config from GetPreset
}
```

### Cancel

Cancel an active freespin campaign by game and reference ID.

```protobuf
message CancelFreespinCommand {
  string game_identity = 1;
  string reference_id = 2;
}
```

---

## WinnerService

Query game winners (settled spins, excluding freespins).

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `FindAll` | `FindAllWinnersQuery` | `FindAllWinnersQuery.Result` | List winners with filters and pagination |

### FindAll

Paginated list of game winners. Returns settled spin results with game, amount, currency, player, and date. Excludes freespin rounds.

```protobuf
// Request
message FindAllWinnersQuery {
  optional string game_identity = 1;    // Filter by game identity
  optional int64 min_amount = 2;        // Minimum win amount (minor units)
  optional int64 max_amount = 3;        // Maximum win amount (minor units)
  optional string currency = 4;         // Filter by currency code
  optional string player_id = 5;        // Filter by player UUID
  optional string from_date = 6;        // Start date (ISO 8601 LocalDateTime)
  optional string to_date = 7;          // End date (ISO 8601 LocalDateTime)
  int32 page_num = 8;                   // Page number (1-based)
  int32 page_size = 9;                  // Items per page
}

// Response
message FindAllWinnersQuery.Result {
  repeated WinnerItemDto items = 1;
  int32 total_items = 2;
  int32 total_pages = 3;
  int32 current_page = 4;
}
```

### WinnerItemDto

```protobuf
message WinnerItemDto {
  GameDto game = 1;         // Full game details
  int64 amount = 2;         // Win amount (minor units)
  string currency = 3;      // Currency code
  string player_id = 4;     // Player UUID
  string date = 5;          // Win date (ISO 8601 LocalDateTime)
}
```

---

## Shared DTOs

### GameDto

```protobuf
message GameDto {
  string identity = 1;
  string name = 2;
  string provider_identity = 3;
  repeated string collection_identities = 4;
  bool bonus_bet_enable = 5;
  bool bonus_wagering_enable = 6;
  repeated string tags = 7;
  bool active = 8;
  map<string, string> images = 9;       // Key → CDN URL
  int32 order = 10;
  string symbol = 11;                   // Aggregator game symbol/code
  string integration = 12;             // Aggregator integration type
  bool free_spin_enable = 14;
  bool free_chip_enable = 15;
  bool jackpot_enable = 16;
  bool demo_enable = 17;
  bool bonus_buy_enable = 18;
  repeated string locales = 19;         // Supported locales
  repeated PlatformDto platforms = 20;  // Supported platforms
  int32 play_lines = 21;               // Number of play lines
}
```

### PlatformDto

```protobuf
enum PlatformDto {
  PLATFORM_UNSPECIFIED = 0;
  PLATFORM_DESKTOP = 1;
  PLATFORM_MOBILE = 2;
  PLATFORM_DOWNLOAD = 3;
}
```

### ProviderDto

```protobuf
message ProviderDto {
  string identity = 1;
  string name = 2;
  map<string, string> images = 3;
  int32 order = 4;
  bool active = 5;
  string aggregator_identity = 6;
}
```

### AggregatorDto

```protobuf
message AggregatorDto {
  string identity = 1;
  string integration = 2;            // "ONEGAMEHUB", "PRAGMATIC", "PATEPLAY"
  google.protobuf.Struct config = 3; // JSON configuration
  bool active = 4;
}
```

### CollectionDto

```protobuf
message CollectionDto {
  string identity = 1;
  map<string, string> name = 2;      // Locale → display name
  map<string, string> images = 3;
  bool active = 4;
  int32 order = 5;
}
```

---

## Error Handling

gRPC errors use standard status codes with the original exception class name in the `x-exception-name` metadata header.

| Exception | gRPC Status | When |
|-----------|-------------|------|
| `NotFoundException` | `NOT_FOUND` | Entity not found by identity |
| `BadRequestException` | `INVALID_ARGUMENT` | Validation failure |
| `ConflictException` | `ALREADY_EXISTS` | Duplicate or conflicting state |
| `ForbiddenException` | `PERMISSION_DENIED` | Unauthorized operation |
| `SystemException` | `INTERNAL` | Unexpected server error |

## Connection

Default endpoint: `localhost:5050` (gRPC)

Environment variables:
- `GAME_CORE_GRPC_HOST` (default: `localhost`)
- `GAME_CORE_GRPC_PORT` (default: `5050`)
