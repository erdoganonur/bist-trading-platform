package com.bisttrading.core.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Domain events represent important business events that have occurred in the domain.
 */
public interface DomainEvent {

    /**
     * Gets the unique identifier of the event.
     *
     * @return Event ID
     */
    UUID getEventId();

    /**
     * Gets the timestamp when the event occurred.
     *
     * @return Event timestamp
     */
    LocalDateTime getOccurredOn();

    /**
     * Gets the version of the event for schema evolution.
     *
     * @return Event version
     */
    default int getEventVersion() {
        return 1;
    }

    /**
     * Gets the name of the event type.
     *
     * @return Event type name
     */
    String getEventType();

    /**
     * Gets the aggregate ID that this event relates to.
     *
     * @return Aggregate ID
     */
    String getAggregateId();
}