package com.ticketing.model.event;

import java.time.Instant;

/**
 * @param id               unique identifier for the event
 * @param name             display name of the event
 * @param date             date and time when the event takes place
 * @param venue            location where the event is held
 * @param totalCapacity    maximum number of tickets for this event
 * @param availableTickets current number of unsold, unreserved tickets
 * @param version          optimistic locking version for conditional writes
 */
public record Event(
        String id,
        String name,
        Instant date,
        String venue,
        int totalCapacity,
        int availableTickets,
        long version
) {

    /**
     * Creates a new Event with available tickets set equal to total capacity.
     * Used when initially creating an event.
     */
    public static Event create(String id, String name, Instant date, String venue, int totalCapacity) {
        return new Event(id, name, date, venue, totalCapacity, totalCapacity, 0L);
    }

    /**
     * Returns a copy of this event with reduced available tickets.
     *
     * @param quantity number of tickets to reserve
     * @return new Event instance with updated availability
     */
    public Event withReducedAvailability(int quantity) {
        return new Event(id, name, date, venue, totalCapacity, availableTickets - quantity, version);
    }

    /**
     * Returns a copy of this event with increased available tickets (for releasing reservations).
     *
     * @param quantity number of tickets to release back
     * @return new Event instance with updated availability
     */
    public Event withIncreasedAvailability(int quantity) {
        return new Event(id, name, date, venue, totalCapacity, availableTickets + quantity, version);
    }

    /**
     * Checks if the requested quantity of tickets is available.
     */
    public boolean hasAvailableTickets(int quantity) {
        return availableTickets >= quantity;
    }
}
