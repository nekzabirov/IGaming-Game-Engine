# Kotlin Code Styling Rules

Follow these conventions exactly when writing or editing Kotlin code in this project.

## Data Class Parameters

- Each parameter on its own line
- Blank line between every parameter
- Trailing comma on last parameter when class implements interfaces

```kotlin
// CORRECT
data class Game(
    val identity: Identity,

    val name: String,

    val provider: Provider,

    override val active: Boolean,
) : Activatable<Game>, Imageable<Game>

// WRONG - no blank lines between params
data class Game(
    val identity: Identity,
    val name: String,
    val provider: Provider,
    override val active: Boolean,
) : Activatable<Game>, Imageable<Game>
```

## Small Data Classes (Pageable, Page)

- When a data class has few params (2-4) with no domain significance per field, params may be on separate lines without blank lines between them:

```kotlin
data class Pageable(
    val page: Int,
    val size: Int
)
```

## Input validation

- No value classes: identifiers and amounts are plain `String`/`Long`, checked once at the API
  boundary with `errors.Valid.*` (`Valid.identity(raw)`, `Valid.amount(raw)`), which throw the
  categorised `DomainException` for the field

## KDoc Comments

- Use `/** ... */` style only for public-facing enums, value objects, and utility classes
- Do not add KDoc to data class models or interfaces — the types and names are self-documenting
- Keep KDoc to a single line when possible

```kotlin
/** Platform type for game variants. */
enum class Platform { ... }
```

## Interfaces

- Minimal — no KDoc, no blank lines between simple members
- Default implementations directly in the interface body when the behavior is universal

```kotlin
interface Activatable {
    var active: Boolean

    fun activate() {
        active = true
    }

    fun deactivate() {
        active = false
    }
}
```

## Class Body Methods

- Blank line between each method
- One-liner overrides use `=` expression body

```kotlin
override fun activate(): Game = this.copy(active = true)

override fun deactivate(): Game = this.copy(active = false)

override fun changeOrder(order: Int): Game = this.copy(order = order)
```

## Companion Objects

- Place at bottom of class body
- Use for factory methods and constants

## Imports

- No wildcard imports
- Sorted alphabetically
- Blank line after `package` declaration, before first import
- No blank lines between imports

## General

- 4-space indentation
- No semicolons
- Use `val` over `var` unless mutation is required by interface contract
- Prefer `emptyList()`, `emptyMap()`, `mutableMapOf()` over constructor calls
- `init` blocks for validation, not business logic
- `require()` for precondition checks in constructors and `init` blocks
