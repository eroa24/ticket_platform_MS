package com.ticketing.usecase.event;

import java.util.Optional;
import java.util.UUID;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;

import reactor.core.publisher.Mono;

public class CreateEventUseCase {

    private final EventGateway eventGateway;

    public CreateEventUseCase(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    /**
     * @param event the event data to create (id will be generated)
     * @return the created event with generated id
     */
    public Mono<Event> execute(Event event) {
        return validateEvent(event)
                .map(this::assignId)
                .flatMap(eventGateway::save);
    }

    private Mono<Event> validateEvent(Event event) {
        return Optional.ofNullable(event)
                .filter(e -> e.name() != null && !e.name().isBlank())
                .filter(e -> e.venue() != null && !e.venue().isBlank())
                .filter(e -> e.date() != null)
                .filter(e -> e.totalCapacity() > 0)
                .map(Mono::just)
                .orElse(Mono.defer(() -> Mono.error(
                        BusinessErrorType.INVALID_REQUEST.build("Event name, venue, date and capacity are required"))));
    }

    private Event assignId(Event event) {
        return Event.create(
                UUID.randomUUID().toString(),
                event.name(),
                event.date(),
                event.venue(),
                event.totalCapacity()
        );
    }
}
