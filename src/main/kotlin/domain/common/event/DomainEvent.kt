package domain.common.event

import kotlinx.serialization.Serializable

/**
 * Base marker interface for all domain events.
 */
interface DomainEvent

/**
 * Integration events are published to message queues (RabbitMQ).
 * These have routing keys for message routing.
 */
@Serializable
sealed interface IntegrationEvent : DomainEvent {
    /**
     * Routing key for message queue.
     */
    val routingKey: String

    /**
     * Timestamp when the event occurred.
     */
    val timestamp: Long
        get() = System.currentTimeMillis()
}

/**
 * Marker interface for session-related integration events.
 */
@Serializable
sealed interface SessionIntegrationEvent : IntegrationEvent

/**
 * Marker interface for spin-related integration events.
 */
@Serializable
sealed interface SpinIntegrationEvent : IntegrationEvent
