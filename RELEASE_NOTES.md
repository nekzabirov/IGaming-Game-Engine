# Release Notes - v1.0.0

**Release Date:** January 2026

---

## Overview

iGambling Core Service v1.1.0 brings major performance optimizations, new gRPC services for Aggregator and Freespin management, enhanced round querying capabilities, and significant infrastructure improvements including Docker optimization and database migrations.

---

## New Features

### Aggregator Management (gRPC)
- **AggregatorService** - Full CRUD operations for game aggregators via gRPC
  - `Create` - Register new aggregators with configuration
  - `Find` - Retrieve aggregator by ID
  - `FindAll` - List aggregators with pagination
  - `Update` - Modify aggregator settings
- Providers can now be associated with specific aggregators

### Freespin Operations (gRPC)
- **GetFreespinPreset** - Retrieve freespin configuration presets
- **CreateFreespin** - Create new freespin awards for players
- **CancelFreespin** - Cancel active freespin awards
- Integrated into `GameGrpcService` for unified game management

### Round Query Enhancements
- **Date Range Filtering** - New `start_at` and `end_at` parameters for `FindAllRound`
- Filter rounds by creation timestamp period
- Enhanced pagination and filtering capabilities

### Collection & Round Management (gRPC)
- New gRPC services for collection management
- Enhanced round management operations
- Currency field added to round records

### Provider Enhancements
- **Game Counts** - Providers now include active game counts in listings
- **Filtering Support** - Case-insensitive querying for provider searches
- Provider-aggregator associations

---

## Performance Optimizations

### In-Memory Balance Caching
- **BalanceCache** implementation with 10-second TTL
- Reduces redundant wallet HTTP calls during high-frequency operations
- Predicted balance caching for faster response handling
- Cache hit logging for monitoring

### Async Processing
- Wallet withdrawals now support async processing with predicted balance
- Parallel execution for round creation and balance validation
- Async event publishing for spin operations

### Database Optimizations
- Single-query steps for round and spin handling
- Repository-based persistence replacing direct database access
- Efficient querying patterns for aggregators

---

## Infrastructure Improvements

### Docker Optimization
- Production-ready Dockerfile with non-root user
- Health check integration
- Updated entrypoint configuration
- Removed development Docker Compose files
- New `.dockerignore` and build scripts

### Database Migrations
- Database migration runner implementation
- Enhanced round management with migration support

### Configuration
- **GRPC_PORT** - Configurable gRPC server port (default: 5050)
- **HTTP_PORT** - Environment variable for HTTP port configuration
- gRPC client publishing script to Maven Local

---

## Refactoring

### gRPC Client Error Handling
- Improved error handling in `GameGrpcService`
- Better error propagation for gRPC clients

### Type Changes
- Replaced `BigInt` with `Int` for improved compatibility
- Simplified numeric handling across the codebase

### Code Cleanup
- Removed unused use cases, repositories, and tests
- Removed aggregator and collection-related legacy modules
- Cleaner codebase with repository-based persistence

---

## gRPC API Changes

### New Service: AggregatorService

| Operation | Description |
|-----------|-------------|
| **Create** | Create a new aggregator |
| **Find** | Get aggregator by ID |
| **FindAll** | List all aggregators with pagination |
| **Update** | Update aggregator configuration |

### Updated Services

| Service | New Operations |
|---------|----------------|
| **GameService** | `GetFreespinPreset`, `CreateFreespin`, `CancelFreespin` |
| **RoundService** | `start_at` and `end_at` date filters in `FindAll` |
| **ProviderService** | Aggregator association support |

---

## Documentation

- Added gRPC API documentation
- Updated proto files with new service definitions

---

## Configuration

### Environment Variables

```bash
# gRPC server port (default: 5050)
GRPC_PORT=5050

# HTTP server port
HTTP_PORT=8080
```

### Balance Cache Settings

The balance cache uses a 10-second TTL by default. This optimizes wallet operations during high-frequency betting scenarios.

---

## Migration Notes

### From v1.0.1

1. **Database Migrations** - Run the migration runner before deployment to ensure schema compatibility

2. **gRPC Port** - The gRPC port is now configurable via `GRPC_PORT` environment variable. Update your deployment configurations if using a non-default port.

3. **BigInt to Int** - If you have custom code using `BigInt` types from the gRPC API, update to use `Int` instead.

4. **Removed Modules** - The following legacy modules have been removed:
   - Standalone aggregator modules (now integrated into core)
   - Collection-related legacy modules

5. **Repository Pattern** - Direct database access has been replaced with repository-based persistence. Update any custom extensions accordingly.

---

## Dependencies

No new external dependencies added. Internal optimizations improved performance without additional overhead.

---

## Known Issues

- Balance cache TTL is not configurable (fixed at 10 seconds)
- Turbo adapters require external Turbo service to be running

---

## Contributors

- nekzabirov
- Claude Code (AI-assisted development)

---

# Previous Release - v1.0.1

**Release Date:** January 2026

---

## Overview

iGambling Core Service v1.0.1 introduces new features for round history querying, image management, and significant architectural improvements with the Saga pattern reorganization and production-ready adapter integrations.

---

## New Features

### Round Details Query
- **GetRoundsDetails** - New gRPC endpoint to retrieve round history with aggregated amounts and game details
- Advanced filtering by player ID, game ID, provider ID, aggregator, date range, and round status
- Pagination support with `offset` and `limit` parameters
- Returns comprehensive round information including:
  - Round status (in-progress, finished)
  - Total bet and win amounts
  - Associated game and provider details
  - Spin transaction history

### Game & Provider Image Management
- **UpdateGameImage** - Upload and update game thumbnail images via gRPC
- **UpdateProviderImage** - Upload and update provider logo images via gRPC
- S3-based file storage with automatic path management
- Supports image replacement and deletion

### gRPC Configuration
- Configurable message size limits (default: 50 MB) for handling large payloads
- Enhanced client integration documentation with configuration examples

---

## Architectural Improvements

### Saga Pattern Reorganization

All spin operations now use a fully modular Saga architecture with dedicated packages and step files:

```
application/saga/spin/
├── place/           # PlaceSpinSaga (6 steps)
│   ├── PlaceSpinSaga.kt
│   └── step/
│       ├── ValidateGameStep.kt
│       ├── FindOrCreateRoundStep.kt
│       ├── ValidateBalanceStep.kt
│       ├── WalletWithdrawStep.kt
│       ├── SavePlaceSpinStep.kt
│       └── PublishSpinPlacedEventStep.kt
│
├── settle/          # SettleSpinSaga (6 steps)
├── end/             # EndSpinSaga (3 steps)
└── rollback/        # RollbackSpinSaga (5 steps)
```

**Benefits:**
- Individual step files for better maintainability and testing
- Clear separation of concerns per operation
- Easier debugging and modification of specific steps
- Consistent compensation handling across all sagas

### Turbo Adapter Integration

Production-ready adapters replacing mock implementations:

| Adapter | Description |
|---------|-------------|
| **TurboWalletAdapter** | HTTP client integration for wallet operations (balance, withdraw, deposit, rollback) |
| **CachePlayerLimitAdapter** | Cache-backed spin limit storage per player (1h TTL) |

**Wallet DTOs Added:**
- `BalanceType` - Real and bonus balance separation
- `BetTransactionRequest` - Structured bet withdrawal requests
- `SettleTransactionRequest` - Structured win deposit requests
- `AccountDto`, `AccountRequest` - Account management

**Player Limit Port Added:**
- `PlayerLimitAdapter` - Spin limit management (save, delete, get)
- `CachePlayerLimitAdapter` - Default cache-backed implementation

---

## Bug Fixes

- Fixed event publishing for spin operations
- Improved currency conversion with proper rounding in `UnitCurrencyAdapter` and `OneGameHubCurrencyAdapter`
- Simplified S3 file handling by returning keys directly instead of CDN URLs

---

## gRPC API Changes

### New Service: RoundService

| Operation | Description |
|-----------|-------------|
| **GetRoundsDetails** | Query round history with filtering and pagination |

### Updated Services

| Service | New Operations |
|---------|----------------|
| **GameService** | `UpdateImage` - Upload game thumbnail |
| **ProviderService** | `UpdateImage` - Upload provider logo |

---

## Domain Events

New event added:

| Event | Routing Key |
|-------|-------------|
| SpinRollbackEvent | `spin.rollback` |

---

## File Adapter

New `FileAdapter` port for file storage operations:

```kotlin
interface FileAdapter {
    suspend fun upload(path: String, content: ByteArray, contentType: String): String
    suspend fun delete(path: String)
    suspend fun exists(path: String): Boolean
}
```

S3 implementation provided in `infrastructure/external/s3/S3FileAdapter.kt`.

---

## Migration Notes

### From v1.0.0

1. **Saga Imports** - If extending sagas, update imports from:
   - `application/saga/spin/PlaceSpinSaga` → `application/saga/spin/place/PlaceSpinSaga`
   - `application/saga/spin/SettleSpinSaga` → `application/saga/spin/settle/SettleSpinSaga`

2. **Removed UseCases** - The following have been converted to Sagas:
   - `PlaceSpinUsecase` → `PlaceSpinSaga`
   - `SettleSpinUsecase` → `SettleSpinSaga`
   - `EndSpinUsecase` → `EndSpinSaga`
   - `RollbackUsecase` → `RollbackSpinSaga`

3. **Adapter Registration** - If using custom adapters, ensure they're registered in `DependencyInjection.kt` to override the default Turbo adapters.

4. **Collection Model** - `ImageMap` has been removed from collections. Image handling is now separate.

---

## Configuration

### S3 File Storage (Required for Image Features)

```kotlin
// Environment variables
S3_ENDPOINT=your-s3-endpoint
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_BUCKET=your-bucket-name
S3_REGION=your-region
```

### gRPC Message Size

```kotlin
// Server configuration (ApiPlugin.kt)
maxInboundMessageSize = 50 * 1024 * 1024  // 50 MB

// Client configuration
ManagedChannelBuilder.forAddress(host, port)
    .maxInboundMessageSize(50 * 1024 * 1024)
    .build()
```

---

## Dependencies

No new external dependencies added. Internal module organization improved.

---

## Known Issues

- S3 adapter requires proper endpoint configuration for non-AWS S3-compatible storage
- Turbo adapters require external Turbo service to be running

---

## Contributors

- nekzabirov
- Claude Code (AI-assisted development)
