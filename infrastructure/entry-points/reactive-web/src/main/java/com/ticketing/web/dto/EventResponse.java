package com.ticketing.web.dto;

import java.time.Instant;

import com.ticketing.model.event.Event;

/**
 * API response DTO for event data.
 * Decouples the domain model from the API contract.
 *
 * @param id               unique event identifier
 * @param name             display name of the event
 * @param date             date and time of the event
 * @param venue            location of the event
 * @param totalCapacity    maximum ticket capacity
 * @param availableTickets current available tickets (excludes reserved and sold)
 */
public record EventResponse(
        String id,
        String name,
        Instant date,
        String venue,
        int totalCapacity,
        int availableTickets
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.id(),
                event.name(),
                event.date(),
                event.venue(),
                event.totalCapacity(),
                event.availableTickets());
    }
}
