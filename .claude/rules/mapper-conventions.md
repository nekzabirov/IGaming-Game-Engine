# Mapper Conventions

Every Entity ↔ domain conversion follows one shape. Do not invent one-offs.

## Canonical Layout

- **Entity → domain**: `fun XEntity.toDomain(): X` as an extension inside `object XMapper`.
- **ResultRow → domain**: `fun ResultRow.toX(): X` as an extension inside `object XMapper`. Distinct name (e.g. `toProvider()`, `toAggregator()`) rather than `toDomain()` because multiple mappers defining `ResultRow.toDomain()` collide when one mapper composes another (`ProviderMapper` calls `AggregatorMapper.toAggregator()` on a joined row).
- **Domain → UpdateBuilder (write path)**: `fun UpdateBuilder<*>.fromDomain(x: X)` — inside the same mapper object.
- **Domain → new Entity**: `fun XEntity.Companion.fromDomain(x: X): XEntity` — factory on the Entity companion when repositories need to create a fresh row.

## Naming rules

- Entity DAO extensions always named `toDomain()`.
- ResultRow extensions named `toX()` where `X` is the aggregate name (`toGame`, `toProvider`, `toCollection`, `toAggregator`).
- Write direction named `fromDomain(x)`.
- File naming: `<Entity>Mapper.kt` under `infrastructure/persistence/mapper/`.

## One file per aggregate

One mapper file per aggregate/entity (e.g. `GameMapper.kt`, `SessionMapper.kt`). Don't group unrelated mappers in a single file.

## Rationale

Consistency lets the next developer find the conversion in zero seconds. It also makes the intent obvious: `toDomain` always returns a domain model; `fromDomain` always builds a persistence shape.

## Existing reference implementations

- `infrastructure/persistence/mapper/GameMapper.kt`
- `infrastructure/persistence/mapper/SessionMapper.kt`
- `infrastructure/persistence/mapper/RoundMapper.kt`
