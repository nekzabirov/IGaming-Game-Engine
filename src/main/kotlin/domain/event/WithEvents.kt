package domain.event

/**
 * Carries a transformed aggregate value alongside the [DomainEvent]s its transition raised.
 *
 * Used by immutable data-class aggregates that perform state changes via `copy()`. The
 * mutator returns `WithEvents<T>(newValue, events)`; the usecase persists `newValue`
 * then publishes `events` after the DB transaction commits.
 */
data class WithEvents<out T>(
    val value: T,

    val events: List<DomainEvent>,
)
