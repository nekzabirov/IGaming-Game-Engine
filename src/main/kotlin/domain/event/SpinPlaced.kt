package domain.event

import domain.model.Spin

data class SpinPlaced(val spin: Spin) : DomainEvent
