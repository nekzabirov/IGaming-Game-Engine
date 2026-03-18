# Exposed Database Rules

All database operations MUST use Exposed DAO Entity pattern for maximum performance and consistency.

## Core Rules

### Always Use Entity DAO, Never Raw SQL
- Use `Entity` / `LongEntity` classes for all reads and writes
- Use `EntityClass.findById()`, `Entity.find {}`, `Entity.new {}` for standard operations
- Use `Table.upsert {}` only for simple insert-or-update (entity-direct handlers)
- Never use raw SQL strings or `exec()` unless absolutely no Exposed alternative exists

### Transaction Handling
- Wrap every DB call in `newSuspendedTransaction { ... }` — never use blocking `transaction { ... }`
- Keep transactions as short as possible — no network calls, no heavy computation inside
- One `newSuspendedTransaction` per handler/repository method; do not nest transactions
- Let exceptions propagate naturally out of the transaction block to trigger rollback

### Eager Loading for Performance
- Use `EntityClass.find { ... }.with(Entity::relation)` to eager-load references and avoid N+1 queries
- For one-to-many and many-to-many relations, always eager-load when you know you'll access them
- Example:
  ```kotlin
  GameEntity.find { GameTable.active eq true }
      .with(GameEntity::provider, GameEntity::collections)
      .map { it.toDomain() }
  ```

### Batch Operations
- Use `Table.batchInsert(items) { ... }` for inserting multiple rows — never loop with `Table.insert` or `Entity.new`
- Use `Table.batchUpsert(items) { ... }` for bulk insert-or-update
- For bulk updates with different values, use `Table.update({ condition }) { ... }` in a single transaction

### Queries and Filtering
- Build conditions with `andWhere {}` for dynamic filters — compose incrementally, don't build giant `where` blocks
- Use `adjustSlice {}` or `adjustSelect {}` when you only need specific columns
- Use `.limit(n).offset(m)` for pagination — always pair with an `orderBy`
- Use `count()` on the query for total count, not `.toList().size`
- Prefer `Entity.find { condition }.empty()` over `count() == 0L` for existence checks

### Index-Aware Querying
- Query by indexed/PK columns whenever possible
- When filtering by foreign keys, use the table column reference (e.g., `SessionTable.gameVariant eq variantId`) not a subquery
- For multi-column lookups, ensure a composite index exists or use the PK

### Column Type Choices
- JSON columns: use Exposed `json()` function with `kotlinx.serialization` serializers
- Enums: use `enumerationByName()` for readability over `enumeration()` (ordinal)
- Timestamps: use `datetime()` with `kotlinx.datetime.LocalDateTime` or Java `Instant`
- Money: `long()` in minor units (cents) — never floating point

### Entity-Direct vs Repository Pattern
- **Entity-direct handlers**: Use for simple CRUD with no domain logic (e.g., `SaveAggregatorCommandHandler`). Work with `Table` / `Entity` directly inside `newSuspendedTransaction`
- **Repository pattern**: Use when domain mapping or business logic is involved. Repository wraps `newSuspendedTransaction` and maps Entity ↔ domain model via mapper extension functions

### What NOT to Do
- Do not call `toList()` before filtering — filter in the query, not in Kotlin
- Do not load entire tables — always apply `where` conditions or pagination
- Do not use `forEach` with individual `save()` calls — use batch operations
- Do not mix blocking `transaction {}` with coroutine code
- Do not create multiple transactions for what could be a single atomic operation
- Do not use `SizedIterable.map { }` outside a transaction — the cursor closes when the transaction ends
