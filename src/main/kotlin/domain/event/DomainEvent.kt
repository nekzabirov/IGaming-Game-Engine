package domain.event

/**
 * Marker for events raised by aggregate roots.
 *
 * Domain events are pure values produced by aggregate state transitions. Aggregates
 * return them inside [WithEvents] from their mutator methods; usecases drain the events
 * after a successful write and call [application.port.external.IEventPort.publish]
 * directly. The publisher (`RabbitMqEventPublisher`) does an exhaustive `when` over the
 * sealed hierarchy to map each event to a routing key + payload.
 *
 * Keep this sealed so the publisher's `when` is compile-time exhaustive — adding a new
 * subtype forces every consumer to handle it.
 */
sealed interface DomainEvent
