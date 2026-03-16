package com.ticketing.usecase.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetEventAvailabilityUseCaseTest {

    @Mock
    private EventGateway eventGateway;

    @InjectMocks
    private GetEventAvailabilityUseCase useCase;

    private static final String EVENT_ID = "event-1";
    private static final Event EVENT = Event.create(EVENT_ID, "Concert", Instant.parse("2027-01-01T00:00:00Z"), "Venue", 200);

    @Test
    void execute_returnsEventWhenFound() {
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));

        StepVerifier.create(useCase.execute(EVENT_ID))
                .assertNext(e -> assertThat(e.id()).isEqualTo(EVENT_ID))
                .verifyComplete();
    }

    @Test
    void execute_failsWithEventNotFoundWhenMissing() {
        when(eventGateway.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("missing"))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.EVENT_NOT_FOUND);
                })
                .verify();
    }

    @Test
    void findAll_returnsAllEvents() {
        var event2 = Event.create("event-2", "Festival", Instant.parse("2027-02-01T00:00:00Z"), "Park", 1000);
        when(eventGateway.findAll()).thenReturn(Flux.just(EVENT, event2));

        StepVerifier.create(useCase.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findAll_returnsEmptyFluxWhenNoEvents() {
        when(eventGateway.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.findAll())
                .verifyComplete();
    }
}
