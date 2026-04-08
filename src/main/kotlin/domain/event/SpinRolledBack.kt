package domain.event

import domain.model.Spin

data class SpinRolledBack(val spin: Spin) : DomainEvent
