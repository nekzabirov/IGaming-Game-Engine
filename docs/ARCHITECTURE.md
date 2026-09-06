# Architecture

casino-engine is a flat Ktor application: one process hosting a gRPC server (the API) and a tiny
HTTP server (`/health`), talking to Postgres through Exposed, to GameHub and pam-engine over gRPC,
to Redis for player limits and to RabbitMQ for events.

```
src/main/kotlin/
├── Application.kt     main() + module(config) — builds every object once and wires it explicitly
├── AppConfig.kt       all environment variables
├── plugins/           configureX(): Databases, Grpc, Messaging, Routing
├── db/                Exposed tables + DAO entities (the model), enums, paging, fuzzy search
├── errors/            DomainException hierarchy (category → gRPC status) and Valid.* input checks
├── clients/           GameHub, Wallet (pam), PlayerLimits (Redis): interface + implementation
├── events/            event payloads + frozen wire serializers, RabbitMQ publisher and consumer
├── services/          the logic: catalog services, LaunchService, WalletService, CatalogSync
├── dto/               entity → proto inside the transaction
└── grpc/              one thin class per gRPC service
```

## Request paths

**Catalog read** (`CasinoGameService.FindAll` etc.): gRPC class validates input → service opens
`dbRead {}` → builds the proto DTO from entities inside the transaction → returns it.

**Launch** (`CasinoGameService.Play`): read the game (one transaction) → save the bet cap in Redis
→ validate active/platform/locale → `GatewayService.LaunchCasino` on the hub → publish
`session.events` → return the URL.

**Money** (`gamehub.v1.WebhookService`, called by the hub): authenticate the operator pair →
transaction 1 (replay check, find-or-create the round, snapshot what the split needs) → wallet
(balance, limit, split, one idempotent `Transact`) → transaction 2 (insert the spin; the unique
index on the leg id decides redelivery races) → publish `spin.events` → answer the wallet's balance.

**Sync** (`bin/sync-catalog`): `GatewayService.ListCasino` → one `batchUpsert` per table whose
`ON CONFLICT DO UPDATE` names only the hub-owned columns.

## Invariants

- No remote call inside a database transaction.
- Events are published after the commit and never fail the caller.
- The event wire shape (`events/Events.kt`) and the exception class names (`errors/Errors.kt`)
  are public contracts.
- Schema belongs to Flyway; `db/Tables.kt` mirrors it.
