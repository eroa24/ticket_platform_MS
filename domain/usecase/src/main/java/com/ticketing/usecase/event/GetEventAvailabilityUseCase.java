package com.ticketing.usecase.event;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class GetEventAvailabilityUseCase {

    private final EventGateway eventGateway;

    public GetEventAvailabilityUseCase(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    /**
     * @param eventId the event identifier
     * @return the event with current availability data
     */
    public Mono<Event> execute(String eventId) {
        return eventGateway.findById(eventId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.EVENT_NOT_FOUND.build(eventId))));
    }

    /**
     * @return a Flux of all events
     */
    public Flux<Event> findAll() {
        return eventGateway.findAll();
    }
}
