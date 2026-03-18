# Seed Game Collections

You are an agent that fetches all games from the casino engine via gRPC, analyzes them, creates ~10 thematic collections, and assigns games to the appropriate collections.

## Tools

You have access to `Bash` for running `grpcurl` commands.

## Proto & gRPC Setup

- **Server**: `localhost:5050` (plaintext, no TLS)
- **Proto import path**: `src/main/proto` (relative to project root)
- **Proto files are at**: `game/v1/service/game.service.proto`, `game/v1/service/collection.service.proto`
- **Package**: `game.v1`

All `grpcurl` commands must use:
```
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto game/v1/service/game.service.proto \
  -proto game/v1/service/collection.service.proto \
  localhost:5050 <method>
```

## Step-by-step Plan

### Step 1: Fetch all games

Call `game.v1.GameService/Batch` with an empty body `{}`:

```bash
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto game/v1/service/game.service.proto \
  localhost:5050 game.v1.GameService/Batch
```

This returns JSON with:
- `items[]` — each has `game` (GameDto) and `provider` (ProviderDto)
- `providers[]` — all unique providers
- `aggregators[]` — all unique aggregators
- `collections[]` — existing collections

Save the full output for analysis.

### Step 2: Analyze games

Look at every game's fields to categorize them:
- `name` — game title (use for keyword matching: e.g. "roulette", "poker", "blackjack", "slot", "baccarat", etc.)
- `tags[]` — explicit tags assigned to the game
- `provider_identity` — which provider made it
- `bonus_buy_enable` — has bonus buy feature
- `jackpot_enable` — jackpot game
- `free_spin_enable` — supports free spins
- `demo_enable` — has demo mode
- `free_chip_enable` — supports free chips
- `bonus_bet_enable` — bonus bet support
- `play_lines` — number of play lines

Based on analysis, decide on ~10 meaningful collections. Examples of good collection themes:
- **Popular Slots** — games with "slot" in name or tags
- **Table Games** — roulette, blackjack, baccarat, poker
- **Jackpot Games** — games where `jackpot_enable` is true
- **Bonus Buy** — games where `bonus_buy_enable` is true
- **Free Spins** — games where `free_spin_enable` is true
- **Live Casino** — games with "live" in name or tags
- **New Games** — recently added / high order games
- **Megaways** — games with "megaways" in name or tags
- **Classic Slots** — games with "classic" in name or low play_lines
- **Crash Games** — games with "crash" in name or tags
- **Card Games** — poker, baccarat, blackjack
- Per-provider collections if a provider has many games

Adapt collection choices to the ACTUAL games you find. Don't create empty collections.

### Step 3: Create collections

For each collection, call `game.v1.CollectionService/Save` with a `CollectionDto`:

```bash
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto game/v1/service/collection.service.proto \
  -d '{
    "identity": "unique-slug-here",
    "name": {"en": "English Name", "ru": "Russian Name"},
    "active": true,
    "order": 1
  }' \
  localhost:5050 game.v1.CollectionService/Save
```

**Identity format**: lowercase kebab-case slug (e.g., `jackpot-games`, `bonus-buy`, `table-games`).
**Name**: provide at least `en` and `ru` locales.
**Order**: sequential from 1 to N.

### Step 4: Add games to collections

For each collection, call `game.v1.CollectionService/UpdateGames`:

```bash
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto game/v1/service/collection.service.proto \
  -d '{
    "identity": "collection-slug",
    "add_games": ["game-identity-1", "game-identity-2"]
  }' \
  localhost:5050 game.v1.CollectionService/UpdateGames
```

Batch all game identities for a collection into a single `UpdateGames` call.

### Step 5: Verify

Call `game.v1.CollectionService/FindAll` to verify collections were created:

```bash
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto game/v1/service/collection.service.proto \
  -d '{"page_num": 0, "page_size": 50}' \
  localhost:5050 game.v1.CollectionService/FindAll
```

## Important Notes

- Run all commands from the project root directory.
- If a collection identity already exists, `Save` will update it — that's fine.
- A game can belong to multiple collections.
- Don't create collections with fewer than 3 games.
- Print a final summary table showing each collection name, identity, and game count.
