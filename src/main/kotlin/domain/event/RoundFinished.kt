package domain.event

import domain.model.Round

data class RoundFinished(val round: Round) : DomainEvent
