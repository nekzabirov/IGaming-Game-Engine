package application.event

import domain.model.Session

data class SessionOpenEvent(val session: Session): ApplicationEvent
