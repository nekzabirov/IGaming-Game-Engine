package domain.event

import domain.model.Spin
import domain.model.SpinType

/**
 * Maps a [Spin]'s transition type to the matching [DomainEvent].
 *
 * Used by the spin lifecycle usecase to drain an event after persistence — keeps the
 * `spin.type → event` mapping in one place so both regular-bet and freespin code paths
 * produce identical events.
 */
fun Spin.toDomainEvent(): DomainEvent = when (type) {
    SpinType.PLACE -> SpinPlaced(this)
    SpinType.SETTLE -> SpinSettled(this)
    SpinType.ROLLBACK -> SpinRolledBack(this)
}
