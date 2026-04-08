package domain.event

import domain.model.Spin

data class SpinSettled(val spin: Spin) : DomainEvent
