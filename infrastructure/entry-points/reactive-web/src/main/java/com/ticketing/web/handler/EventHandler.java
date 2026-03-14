package com.ticketing.web.handler;

import com.ticketing.model.event.Event;
import com.ticketing.usecase.event.CreateEventUseCase;
import com.ticketing.usecase.event.GetEventAvailabilityUseCase;
import com.ticketing.web.dto.CreateEventRequest;
import com.ticketing.web.dto.EventResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class EventHandler {

    private final CreateEventUseCase createEventUseCase;
    private final GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    public EventHandler(CreateEventUseCase createEventUseCase,
                        GetEventAvailabilityUseCase getEventAvailabilityUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.getEventAvailabilityUseCase = getEventAvailabilityUseCase;
    }

    public Mono<ServerResponse> createEvent(ServerRequest request) {
        return request.bodyToMono(CreateEventRequest.class)
                .map(this::toDomainEvent)
                .flatMap(createEventUseCase::execute)
                .map(EventResponse::from)
                .flatMap(event -> ServerResponse.status(201).bodyValue(event));
    }

    public Mono<ServerResponse> getAvailability(ServerRequest request) {
        var eventId = request.pathVariable("id");
        return getEventAvailabilityUseCase.execute(eventId)
                .map(EventResponse::from)
                .flatMap(event -> ServerResponse.ok().bodyValue(event));
    }

    public Mono<ServerResponse> getAllEvents(ServerRequest request) {
        return ServerResponse.ok().body(
                getEventAvailabilityUseCase.findAll().map(EventResponse::from),
                EventResponse.class);
    }

    private Event toDomainEvent(CreateEventRequest req) {
        return Event.create(null, req.name(), req.date(), req.venue(), req.totalCapacity());
    }
}
