# Domain Model Loading Rules

When loading domain models from the database (infrastructure layer), **always populate all dependencies** — including `lateinit` fields.

## Rule

Every domain model returned from a repository or mapper must have all its relationships fully loaded:

- **Constructor parameters**: `provider`, `collections`, etc. must be populated, never left as defaults when the real data exists
- **`lateinit var` fields**: Must be assigned after construction. A `lateinit` field (e.g., `Game.variant`) is still a required dependency — it just can't be set in the constructor due to circular or conditional wiring
- **Eager load** all referenced entities in the query to avoid N+1 and to ensure the domain model is complete

## Example

`Game` has `provider: Provider`, `collections: List<Collection>`, and `lateinit var variant: GameVariant`. When querying games from the database, the result must include all three — even though `variant` is lateinit:

```kotlin
// CORRECT — all dependencies loaded
val game = row.toGame(collections)
game.variant = variantEntity.toDomain()

// WRONG — variant left uninitialized
val game = row.toGame(collections)
// game.variant not set — will throw UninitializedPropertyAccessException
```

## When writing queries that return domain models

- Join or eager-load all related tables (use `.with()` for Entity DAO or explicit joins for raw queries)
- After mapping to the domain model, assign any `lateinit` fields before returning
- If a query intentionally skips a relationship (e.g., a summary query that doesn't need variants), document why in a comment
