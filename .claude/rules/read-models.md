# Read Models (Query Views)

CQRS separates the write side (commands → domain aggregates) from the read side (queries → views). In this codebase:

## Where each type lives

- **Domain models** (`domain/model/`) — write-side aggregates. Only appear in command handler return types and repository signatures.
- **Query views** — read-side DTOs declared as **top-level data classes in the same file as the query** (e.g. `CollectionView` in `application/query/collection/FindCollectionQuery.kt`, `LastWin` in `application/query/winner/LastWinnerQuery.kt`). They can denormalize freely, join across aggregates, and carry computed fields (counts, sums).

There is **no `application/projection/` package**. Locality wins over an extra folder.

## When to create a view type

Create a `XView` data class next to the query if the query returns:

- Computed fields not present on the aggregate (e.g. game count per collection)
- A denormalized join across aggregates (e.g. game with provider name + aggregator name inlined)
- A shape that differs from the aggregate (e.g. only 3 of the aggregate's 12 fields)

## When to reuse the domain model

If a query returns a pure aggregate (full `Game`, full `Session`) with its relationships, return the domain class directly. Example: `BatchCollectionQuery : IQuery<List<Collection>>` — no view needed.

## Naming and ownership

- `XView` for an enriched read shape (e.g. `CollectionView`, `ProviderView`, `RoundView`)
- `LastWin`-style semantic names are also acceptable when the meaning is clearer than `<Name>View`
- When `Find*Query` and `FindAll*Query` share the same view shape, **the `Find*Query.kt` file owns the type** and `FindAll*Query.kt` references it via the same Kotlin package (no import needed). Single source of truth, zero duplication.

## Rationale

Mixing view DTOs with domain models breaks invariants: a view might have a nullable field that the aggregate forbids. Keeping the view in the query file (not in domain) makes the split obvious; keeping it next to the query (not in a separate `projection/` folder) keeps it discoverable.
