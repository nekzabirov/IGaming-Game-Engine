package application.port.external

import domain.event.DomainEvent

interface IEventPort {

    suspend fun publish(event: DomainEvent)

    suspend fun publishAll(events: Iterable<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
