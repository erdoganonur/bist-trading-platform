package com.bisttrading.core.domain.events;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for domain events.
 * Provides common functionality for all domain events.
 */
@Getter
public abstract class BaseDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;

    protected BaseDomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID boş olamaz");
    }

    protected BaseDomainEvent(UUID eventId, LocalDateTime occurredOn, String aggregateId) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID boş olamaz");
        this.occurredOn = Objects.requireNonNull(occurredOn, "Occurrence time boş olamaz");
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID boş olamaz");
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseDomainEvent that = (BaseDomainEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return String.format("%s{eventId=%s, aggregateId='%s', occurredOn=%s}",
            getEventType(), eventId, aggregateId, occurredOn);
    }
}