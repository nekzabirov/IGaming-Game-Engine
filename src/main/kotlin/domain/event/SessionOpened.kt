package domain.event

import domain.model.Session

data class SessionOpened(val session: Session) : DomainEvent
