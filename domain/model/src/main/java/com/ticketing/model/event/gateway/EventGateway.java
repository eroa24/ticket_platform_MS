package com.ticketing.model.event.gateway;

import com.ticketing.model.event.Event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventGateway {

    /**
     * @param event the event to save
     * @return the saved event
     */
    Mono<Event> save(Event event);

    /**
     * @param id the event id
     * @return the event if found, or empty Mono
     */
    Mono<Event> findById(String id);

    /**
     * @return a Flux of all events
     */
    Flux<Event> findAll();

    /**
     * @param eventId          the event to update
     * @param ticketsDelta     the change in available tickets (negative for reservation, positive for release)
     * @param expectedVersion  the expected version for optimistic locking
     * @return the updated event
     */
    Mono<Event> updateAvailableTickets(String eventId, int ticketsDelta, long expectedVersion);
}
