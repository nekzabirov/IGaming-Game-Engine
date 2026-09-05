# Casino Engine gRPC API

Package: `game.v1` | Java package: `com.nekgamebling.game.v1`

---

## CasinoGameService

CasinoGame catalog management, launching, and player favorites. Games are contract artefacts and cannot be deleted once registered.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `SaveCasinoGameCommand` | `Empty` | Create or update a game |
| `Find` | `FindCasinoGameQuery` | `FindCasinoGameQuery.Result` | Get a single game by identity |
| `FindAll` | `FindAllCasinoGameQuery` | `CasinoGamePageDto` | List/filter games with pagination |
| `FindAllActiveRtp` | `FindAllActiveRtpCasinoGameQuery` | `CasinoGamePageDto` | List active games by RTP bucket (HOT/COLD) |
| `FindTagsAll` | `FindAllCasinoGameTagQuery` | `FindAllCasinoGameTagQuery.Result` | Paged alphabetical list of distinct tags across active games |
| `Batch` | `BatchCasinoGameQuery` | `BatchCasinoGameQuery.Result` | Batch fetch games |
| `UpdateImage` | `UpdateCasinoGameImageCommand` | `Empty` | Attach/replace a game image URL |
| `Play` | `PlayCasinoGameCommand` | `PlayCasinoGameCommand.Result` | Open a real-money game session |
| `OpenDemo` | `OpenDemoQuery` | `OpenDemoQuery.Result` | Open a demo game session |
| `AddFavourite` | `CasinoGameFavouriteCommand` | `Empty` | Add game to player favorites |
| `RemoveFavourite` | `CasinoGameFavouriteCommand` | `Empty` | Remove game from player favorites |
| `FindAllPlayerFavourite` | `FindAllCasinoGamePlayerFavouriteQuery` | `CasinoGamePageDto` | List a player's favourite games with the same filter/shape as `FindAll` |
| `FindAllPlayerLast` | `FindAllCasinoGamePlayerLastQuery` | `CasinoGamePageDto` | List the games a player recently played (deduplicated, newest session first) |

### Save

Create or update a game's editable properties.

```protobuf
message SaveCasinoGameCommand {
  string identity = 1;              // CasinoGame unique identifier
  string name = 2;                  // Display name
  bool bonus_bet_enable = 3;        // Allow bonus bets
  bool bonus_wagering_enable = 4;   // Allow bonus wagering
  repeated string tags = 5;         // Searchable tags
  string provider_identity = 6;     // Parent provider identity
  bool active = 7;                  // Whether the game is active (playable/visible)
}
```

### Find

Returns a game with its provider, aggregator, and collections.

```protobuf
// Request
message FindCasinoGameQuery {
  string identity = 1;
}

// Response
message FindCasinoGameQuery.Result {
  CasinoGameDto item = 1;
  CasinoProviderDto provider = 2;
  AggregatorDto aggregator = 3;
  repeated CollectionDto collections = 4;
}
```

### FindAll

Paginated game listing with filters. Returns the shared `CasinoGamePageDto` — the same response type every paged game-listing RPC uses.

```protobuf
// Request
message FindAllCasinoGameQuery {
  CasinoGameFilter filter = 1;                         // Filter criteria
  int32 page_num = 2;                            // Page number (0-based)
  int32 page_size = 3;                           // Items per page
}
```

`CasinoGameFilter` and `CasinoGamePageDto` both live in `game/v1/dto/` — see [Shared game-listing DTOs](#shared-game-listing-dtos) below.

### FindAllActiveRtp

Paginated listing of ACTIVE games bucketed by RTP relative to the default (96).
`TYPE_HOT` = rtp > 96 ordered rtp DESC, `TYPE_COLD` = rtp < 96 ordered rtp ASC;
catalog `order` is the secondary sort key (ASC) in both cases. `TYPE_UNSPECIFIED`
is rejected with `INVALID_ARGUMENT`.

```protobuf
// Request
message FindAllActiveRtpCasinoGameQuery {
  enum Type {
    TYPE_UNSPECIFIED = 0;
    TYPE_HOT = 1;
    TYPE_COLD = 2;
  }
  Type type = 1;                                 // RTP bucket
  CasinoGameFilter filter = 2;                         // Extra filter criteria
  int32 page_num = 3;                            // Page number (0-based)
  int32 page_size = 4;                           // Items per page
}
```

### Batch

Fetch games by identities. Response structure matches `FindAll`.

```protobuf
// Request
message BatchCasinoGameQuery {
  repeated string identities = 1;  // game identities to fetch
}

// Response
message BatchCasinoGameQuery.Result {
  repeated CasinoGameDto items = 1;
  repeated CasinoProviderDto providers = 2;
  repeated AggregatorDto aggregators = 3;
  repeated CollectionDto collections = 4;
}
```

### UpdateImage

Attach or replace a game image URL by key (e.g. `"thumbnail"`, `"banner"`).
The engine stores the URL verbatim — callers upload the file to object storage
themselves and pass the final public URL.

```protobuf
message UpdateCasinoGameImageCommand {
  string identity = 1;     // CasinoGame identity
  string key = 2;          // Image key (e.g. "thumbnail")
  string url = 5;          // Full public URL of the image
}
```

### Play

Open a real-money game session. Returns a launch URL the player should be redirected to, plus the
session token the engine minted for it.

```protobuf
// Request
message PlayCasinoGameCommand {
  string identity = 1;                      // CasinoGame identity
  string player_id = 2;                     // Player UUID
  string locale = 3;                        // Player locale (e.g. "en")
  PlatformDto platform = 4;                 // DESKTOP / MOBILE / DOWNLOAD
  string currency = 5;                      // Currency code (e.g. "USD")
  optional int64 max_spin_place_amount = 6; // Max bet limit (minor units)
}

// Response
message PlayCasinoGameCommand.Result {
  string launch_url = 1;
  string session_token = 2;  // Our session token (not the provider's external one)
}
```

### OpenDemo

Open a demo (free-play) game session. No player authentication required.

```protobuf
// Request
message OpenDemoQuery {
  string identity = 1;       // CasinoGame identity
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
message CasinoGameFavouriteCommand {
  string identity = 1;   // CasinoGame identity
  string player_id = 2;  // Player UUID
}
```

### FindAllPlayerFavourite

Paginated listing of the games a specific player has favourited. Uses the shared `CasinoGameFilter` and returns the shared `CasinoGamePageDto` — the only extra input is `player_id`.

```protobuf
// Request
message FindAllCasinoGamePlayerFavouriteQuery {
  string player_id = 1;      // Player UUID whose favourites to read
  CasinoGameFilter filter = 2;     // Shared filter — see Shared game-listing DTOs
  int32 page_num = 3;
  int32 page_size = 4;
}
```

Response: `CasinoGamePageDto` (see [Shared game-listing DTOs](#shared-game-listing-dtos)).

### FindAllPlayerLast

Paginated listing of the games a specific player has recently played, derived from the player's sessions. Games are de-duplicated (one row per game) and ordered by the most recent session first.

```protobuf
// Request
message FindAllCasinoGamePlayerLastQuery {
  string player_id = 1;      // Player UUID whose recent games to read
  int32 page_num = 2;
  int32 page_size = 3;
}
```

Response: `CasinoGamePageDto` (see [Shared game-listing DTOs](#shared-game-listing-dtos)).

---

## CasinoProviderService

CasinoGame provider management (e.g. "Pragmatic Play", "NetEnt").

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `SaveCasinoProviderCommand` | `Empty` | Create or update a provider |
| `Find` | `FindCasinoProviderQuery` | `FindCasinoProviderQuery.Result` | Get provider with denormalized aggregator |
| `FindAll` | `FindAllCasinoProviderQuery` | `FindAllCasinoProviderQuery.Result` | List/filter providers with pagination |
| `FindTagsAll` | `FindAllCasinoProviderTagQuery` | `FindAllCasinoProviderTagQuery.Result` | Paged alphabetical list of distinct tags across active providers |
| `Batch` | `BatchCasinoProviderQuery` | `BatchCasinoProviderQuery.Result` | Batch fetch providers by identities |
| `UpdateImage` | `UpdateCasinoProviderImageCommand` | `Empty` | Attach/replace a provider image URL |

Providers are contract artefacts and cannot be deleted once registered.

### Save

Create or update a provider with explicit typed fields. Image URLs are managed separately via `UpdateImage`.

```protobuf
message SaveCasinoProviderCommand {
  string identity = 1;              // CasinoProvider unique identifier
  string name = 2;                  // Display name
  int32 order = 3;                  // Sort order
  bool active = 4;                  // Active status
  string aggregator_identity = 5;   // Parent aggregator identity
  repeated string blocked_country = 6; // ISO country codes blocked for this provider
  repeated string tags = 7;         // Free-form tags (e.g. "live")
}
```

### Find

Returns a provider with its aggregator denormalized.

```protobuf
// Request
message FindCasinoProviderQuery {
  string identity = 1;
}

// Response
message FindCasinoProviderQuery.Result {
  CasinoProviderDto item = 1;
  AggregatorDto aggregator = 2;
}
```

### FindAll

Paginated provider listing. Filter criteria are wrapped in a dedicated `CasinoProviderFilter` sub-message.

```protobuf
message CasinoProviderFilter {
  string query = 1;                              // Free-text search
  optional bool active = 2;                      // Filter by status
  optional string aggregator_identity = 3;       // Filter by aggregator
  repeated string in_collection_identities = 4;  // Filter by collection identities
                                                 // (providers having games in these collections)
  repeated string tags = 5;                      // Match any of these tags
}

// Request
message FindAllCasinoProviderQuery {
  CasinoProviderFilter filter = 1;
  int32 page_num = 2;
  int32 page_size = 3;
}

// Response
message FindAllCasinoProviderQuery.Result {
  repeated CasinoProviderDto items = 1;
  repeated AggregatorDto aggregators = 2;        // All referenced aggregators (denormalized)
  int32 total_items = 3;
}
```

### Batch

Fetch providers by identities. Returns providers with their aggregators denormalized.

```protobuf
// Request
message BatchCasinoProviderQuery {
  repeated string identities = 1;  // provider identities to fetch
}

// Response
message BatchCasinoProviderQuery.Result {
  repeated CasinoProviderDto items = 1;
  repeated AggregatorDto aggregators = 2;
}
```

### UpdateImage

```protobuf
message UpdateCasinoProviderImageCommand {
  string identity = 1;
  string key = 2;
  string url = 5;   // Full public URL of the image
}
```

---

## AggregatorService

Aggregator configuration management (e.g. ONEGAMEHUB, PRAGMATIC, PATEPLAY). Aggregator has no images, so there is no `UpdateImage` RPC.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `SaveAggregatorCommand` | `Empty` | Create or update an aggregator |
| `Find` | `FindAggregatorQuery` | `FindAggregatorQuery.Result` | Get aggregator by identity |
| `FindAll` | `FindAllAggregatorQuery` | `FindAllAggregatorQuery.Result` | List/filter aggregators with pagination |
| `Batch` | `BatchAggregatorQuery` | `BatchAggregatorQuery.Result` | Batch fetch aggregators by identities |
| `Delete` | `DeleteAggregatorCommand` | `Empty` | Hard-delete an aggregator by identity |

### Save

Create or update an aggregator with explicit typed fields. The `config` field is a `google.protobuf.Struct` carrying aggregator-specific settings (API keys, endpoints, etc.) — handed to the aggregator adapter at runtime.

```protobuf
message SaveAggregatorCommand {
  string identity = 1;               // Aggregator unique identifier
  string integration = 2;            // Integration type: "ONEGAMEHUB", "PRAGMATIC", "PATEPLAY"
  google.protobuf.Struct config = 3; // Aggregator-specific JSON configuration
  bool active = 4;                   // Active status
  AggregatorTypeDto type = 5;        // Product type; UNSPECIFIED is treated as CASINO
}
```

### Find

Returns the aggregator wrapped in a nested `Result` message for shape consistency with the other catalog services.

```protobuf
// Request
message FindAggregatorQuery {
  string identity = 1;
}

// Response
message FindAggregatorQuery.Result {
  AggregatorDto item = 1;
}
```

### FindAll

Paginated aggregator listing. Filter criteria live in a dedicated `AggregatorFilter` sub-message.

```protobuf
message AggregatorFilter {
  string query = 1;                  // Free-text search
  optional bool active = 2;          // Filter by status
  optional string integration = 3;   // Filter by integration type
}

// Request
message FindAllAggregatorQuery {
  AggregatorFilter filter = 1;
  int32 page_num = 2;
  int32 page_size = 3;
}

// Response
message FindAllAggregatorQuery.Result {
  repeated AggregatorDto items = 1;
  int32 total_items = 2;
}
```

### Batch

Fetch aggregators by identities. Missing identities are silently skipped — the response contains only the rows that were found.

```protobuf
// Request
message BatchAggregatorQuery {
  repeated string identities = 1;
}

// Response
message BatchAggregatorQuery.Result {
  repeated AggregatorDto items = 1;
}
```

### Delete

Hard-delete an aggregator by identity. Raises `AggregatorNotFoundException` (`NOT_FOUND`) if missing. Callers must delete dependent providers first.

```protobuf
message DeleteAggregatorCommand {
  string identity = 1;
}
```

---

## CollectionService

CasinoGame collection management (e.g. "Hot Games", "New Releases") with multi-language support.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Save` | `SaveCollectionCommand` | `Empty` | Create or update a collection |
| `Find` | `FindCollectionQuery` | `FindCollectionQuery.Result` | Get a single collection by identity |
| `FindAll` | `FindAllCollectionQuery` | `FindAllCollectionQuery.Result` | List/filter collections with pagination |
| `Batch` | `BatchCollectionQuery` | `BatchCollectionQuery.Result` | Batch fetch collections by identities |
| `FindAllCasinoGame` | `FindAllCasinoGameCollectionQuery` | `CasinoGamePageDto` | List games that belong to a given collection, ordered by per-collection position |
| `AddCasinoGame` | `AddCollectionCasinoGameCommand` | `Empty` | Add one game to a collection (idempotent; appended at end) |
| `RemoveCasinoGame` | `RemoveCollectionCasinoGameCommand` | `Empty` | Remove one game from a collection (idempotent) |
| `UpdateCasinoGameOrder` | `UpdateCollectionCasinoGameOrderCommand` | `Empty` | Set one game's per-collection sort position |
| `UpdateImage` | `UpdateCollectionImageCommand` | `Empty` | Attach/replace a collection image URL |

Collections are contract artefacts and cannot be deleted once created.

### Save

Create or update a collection with explicit typed fields. Image URLs are managed separately via `UpdateImage`.

```protobuf
message SaveCollectionCommand {
  string identity = 1;              // Collection unique identifier
  map<string, string> name = 2;     // Locale → display name (e.g. {"en": "Hot Games", "de": "Heisse Spiele"})
  bool active = 3;
  int32 order = 4;                  // Sort order
  repeated string tags = 5;         // Collection's own tags (full replace on every Save)
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
}
```

### FindAll

Paginated collection listing. Filter criteria live in a dedicated `CollectionFilter` sub-message.

```protobuf
message CollectionFilter {
  string query = 1;                              // Free-text search
  optional bool active = 2;                      // Filter by status
  repeated string in_tags = 3;                   // Filter by the collection's OWN tags
                                                 // (ANY-of match, same semantics as CasinoGameFilter.tags)
  repeated string in_provider_identities = 4;    // Filter by provider identities
                                                 // (collections containing games from these providers)
}

// Request
message FindAllCollectionQuery {
  CollectionFilter filter = 1;
  int32 page_num = 2;
  int32 page_size = 3;
}

// Response
message FindAllCollectionQuery.Result {
  repeated CollectionDto items = 1;
  int32 total_items = 2;
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
  repeated CollectionDto items = 1;
}
```

### FindAllCasinoGame

Paginated listing of the games that belong to a specific collection. Uses the shared `CasinoGameFilter` and returns the shared `CasinoGamePageDto` — the only extra input is `collection_identity`.

```protobuf
// Request
message FindAllCasinoGameCollectionQuery {
  string collection_identity = 1;  // Collection whose games to read
  CasinoGameFilter filter = 2;           // Shared filter — see Shared game-listing DTOs
  int32 page_num = 3;
  int32 page_size = 4;
}
```

Response: `CasinoGamePageDto` (see [Shared game-listing DTOs](#shared-game-listing-dtos)).

### AddCasinoGame / RemoveCasinoGame / UpdateCasinoGameOrder

Three focused single-game RPCs manage game-in-collection membership and per-collection ordering. Each accepts one `game_identity` — batch operations are not supported by design.

All three fail with `NOT_FOUND` + `x-exception-name=CollectionNotFoundException` if the collection does not exist, or `x-exception-name=CasinoGameNotFoundException` if the game does not exist. `UpdateCasinoGameOrder` additionally returns `CasinoGameNotFoundException` if the game is not currently a member of the collection.

#### AddCasinoGame

Add a single game to a collection. **Idempotent** — if the game is already in the collection, no-op. On first insert, the new row is appended at the end (`sort_order = max(existing) + 1`, or `0` when the collection is empty).

```protobuf
message AddCollectionCasinoGameCommand {
  string identity = 1;       // Collection identity
  string game_identity = 2;  // CasinoGame to add
}
```

#### RemoveCasinoGame

Remove a single game from a collection. **Idempotent** — if the game is not currently a member, no-op. Remaining games keep their existing `sort_order` values (no compaction — holes in the sequence are harmless because the read side only uses it for `ORDER BY`).

```protobuf
message RemoveCollectionCasinoGameCommand {
  string identity = 1;       // Collection identity
  string game_identity = 2;  // CasinoGame to remove
}
```

#### UpdateCasinoGameOrder

Set the per-collection display position for a single game. Fails if the game is not currently a member of the collection — callers must `AddCasinoGame` first.

```protobuf
message UpdateCollectionCasinoGameOrderCommand {
  string identity = 1;       // Collection identity
  string game_identity = 2;  // CasinoGame whose position to update
  int32 order = 3;           // New per-collection sort position
}
```

`CollectionService.FindAllCasinoGame` returns games ordered by `sort_order ASC`, with ties broken deterministically on `CasinoGameTable.id`.

### UpdateImage

```protobuf
message UpdateCollectionImageCommand {
  string identity = 1;
  string key = 2;
  string url = 5;   // Full public URL of the image
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
  reserved 1;                           // was game_identity — use `filter` instead
  optional int64 min_amount = 2;        // Minimum win amount (minor units)
  optional int64 max_amount = 3;        // Maximum win amount (minor units)
  optional string currency = 4;         // Filter by currency code
  optional string player_id = 5;        // Filter by player UUID
  optional string from_date = 6;        // Start date (ISO 8601 LocalDateTime)
  optional string to_date = 7;          // End date (ISO 8601 LocalDateTime)
  int32 page_num = 8;                   // Page number (1-based)
  int32 page_size = 9;                  // Items per page
  CasinoGameFilter filter = 10;               // Restrict to wins on games matching this filter
  WinnerSortDto sort = 11;              // Ordering (always descending)
}

// Ordering of the feed. Always DESCENDING — a winners board is either "latest"
// or "biggest"; ascending has no product meaning, so there is no direction flag.
// Ties break on the spin id so paging is stable.
enum WinnerSortDto {
  WINNER_SORT_UNSPECIFIED = 0;          // treated as WINNER_SORT_DATE
  WINNER_SORT_DATE = 1;                 // newest wins first
  WINNER_SORT_AMOUNT = 2;               // biggest wins first
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
  CasinoGameDto game = 1;         // Full game details
  int64 amount = 2;         // Win amount (minor units)
  string currency = 3;      // Currency code
  string player_id = 4;     // Player UUID
  string date = 5;          // Win date (ISO 8601 LocalDateTime)
}
```

---

## SportbookService

Opens the sportbook for a player. Resolves the single active `SPORTBOOK` aggregator, mints a session with a one-time public token and returns what the frontend needs to boot the provider SDK.

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `Open` | `OpenSportbookCommand` | `OpenSportbookCommand.Result` | Open a sportbook session for a player |
| `Init` | `InitSportbookQuery` | `InitSportbookQuery.Result` | Anonymous SDK bootstrap — no player, no session |

### Open

```protobuf
// Request
message OpenSportbookCommand {
  string player_id = 1;             // Player UUID
  string currency = 2;              // Player wallet currency (ISO 4217 / crypto, e.g. "USD")
}

// Response
message OpenSportbookCommand.Result {
  string integration = 1;           // Aggregator integration key (e.g. "01TECHSPORT") — which SDK to load
  map<string, string> data = 2;     // SDK init payload (e.g. public token, partnerId)
}
```

Errors: `NOT_FOUND` when no active `SPORTBOOK` aggregator exists; `INVALID_ARGUMENT` for a sportbook-incapable aggregator (`SportbookNotSupportedException`).

### Init

```protobuf
// Request
message InitSportbookQuery {}

// Response
message InitSportbookQuery.Result {
  string integration = 1;           // Aggregator integration key (e.g. "01TECHSPORT") — which SDK to load
  map<string, string> data = 2;     // Anonymous SDK init payload (e.g. partnerId, apiUrl)
}
```

The anonymous half of the sportbook bootstrap: resolves the active `SPORTBOOK` aggregator and returns the SDK init payload without a player or session — a guest browses the line with exactly this. The aggregator config is the single source of these values; frontends must not duplicate them in their own env. Errors: same as `Open`.

---

## Shared DTOs

### CasinoGameDto

```protobuf
message CasinoGameDto {
  string identity = 1;
  string name = 2;
  string provider_identity = 3;
  repeated string collection_identities = 4;
  bool bonus_bet_enable = 5;
  bool bonus_wagering_enable = 6;
  repeated string tags = 7;             // MERGED: the hub's synced tags plus custom_tags
  bool active = 8;
  map<string, string> images = 9;       // Key → full public image URL (merged the same way)
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
  double rtp = 22;                     // Synced from GameHub. 0 = no measured value, not 0%
  repeated string custom_tags = 23;    // The LOCAL half of `tags` — see below
}
```

`tags` is the merged view and `custom_tags` is the local subset of it — the editorial tags the catalog
sync never touches. Both are on the wire because `UpdateTags` replaces the local list WHOLE: a caller
that sees only the merge cannot call it correctly, since adding one tag would promote every hub tag
into the local column and removing a hub tag would be a silent no-op. A player-facing client reads
`tags` and ignores the seam; an operator editing them needs it.

### PlatformDto

```protobuf
enum PlatformDto {
  PLATFORM_UNSPECIFIED = 0;
  PLATFORM_DESKTOP = 1;
  PLATFORM_MOBILE = 2;
  PLATFORM_DOWNLOAD = 3;
}
```

### CasinoProviderDto

```protobuf
message CasinoProviderDto {
  string identity = 1;
  string name = 2;
  map<string, string> images = 3;
  int32 order = 4;
  bool active = 5;
  reserved 6;                          // was aggregator_identity — the Aggregator domain is gone
  repeated string blocked_country = 7; // ISO country codes blocked for this provider
  repeated string tags = 8;            // MERGED: the hub's synced tags plus custom_tags
  repeated string custom_tags = 9;     // The LOCAL half — same reason as CasinoGameDto.custom_tags
}
```

### AggregatorDto

```protobuf
enum AggregatorTypeDto {
  AGGREGATOR_TYPE_UNSPECIFIED = 0;
  AGGREGATOR_TYPE_CASINO = 1;
  AGGREGATOR_TYPE_SPORTBOOK = 2;
}

message AggregatorDto {
  string identity = 1;
  string integration = 2;            // "ONEGAMEHUB", "PRAGMATIC", "PATEPLAY"
  google.protobuf.Struct config = 3; // JSON configuration
  bool active = 4;
  AggregatorTypeDto type = 5;        // Product type; UNSPECIFIED is treated as CASINO
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
  repeated string tags = 6;          // Collection's own tags
}
```

## Shared game-listing DTOs

Both `CasinoGameFilter` and `CasinoGamePageDto` live in dedicated DTO files (`game/v1/dto/casino_game_filter.dto.proto`, `game/v1/dto/casino_game_page.dto.proto`) so every game-listing RPC can share the same request filter and response shape without any service importing another service.

### CasinoGameFilter

Reusable filter used by `CasinoGameService.FindAll`, `CasinoGameService.FindAllPlayerFavourite`, and `CollectionService.FindAllCasinoGame`. Every boolean is tri-state via `optional` — an unset field means "do not filter on this flag".

```protobuf
message CasinoGameFilter {
  string query = 1;                      // Free-text search
  optional bool active = 2;
  optional string provider_identity = 3;
  repeated string tags = 4;
  optional bool bonus_bet_enable = 5;
  optional bool bonus_wagering_enable = 6;
  optional bool free_spin_enable = 7;
  optional bool free_chip_enable = 8;
  optional bool jackpot_enable = 9;
  optional bool demo_enable = 10;
  optional bool bonus_buy_enable = 11;
  optional string collection_identity = 12;   // Restrict to one collection's members
}
```

`collection_identity` restricts the listing to the games that belong to that collection. In `CasinoGameService.FindAll` it also switches the ordering: instead of the catalog-wide `sort_order`, results come back in the collection's own curated order — the position set by `CollectionService.AddCasinoGame` / `UpdateCasinoGameOrder` — so rendering a lobby rail is a single `FindAll` call.

### CasinoGamePageDto

Shared paged response for every paged game-listing RPC. The denormalized sets are joined back to each item by identity, so the wire payload never ships the same provider/aggregator/collection twice.

```protobuf
message CasinoGamePageDto {
  repeated CasinoGameDto items = 1;
  repeated CasinoProviderDto providers = 2;     // All referenced providers (denormalized)
  repeated AggregatorDto aggregators = 3; // All referenced aggregators (denormalized)
  repeated CollectionDto collections = 4; // All referenced collections (denormalized)
  int32 total_items = 5;                  // Total count for pagination
}
```

Join rules:
- `CasinoGameDto.provider_identity` → `providers`
- `CasinoProviderDto.aggregator_identity` → `aggregators`
- `CasinoGameDto.collection_identities` → `collections`

Note: `BatchCasinoGameQuery` has its own nested Result because it is not paged — it omits `total_items`.

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
