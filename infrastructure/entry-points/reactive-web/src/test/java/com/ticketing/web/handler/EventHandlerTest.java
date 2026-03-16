package com.ticketing.web.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.ticketing.model.event.Event;
import com.ticketing.usecase.event.CreateEventUseCase;
import com.ticketing.usecase.event.GetEventAvailabilityUseCase;
import com.ticketing.web.dto.EventResponse;
import com.ticketing.web.router.EventRouter;
import com.ticketing.web.validation.RequestValidator;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EventHandlerTest {

    @Mock
    private CreateEventUseCase createEventUseCase;
    @Mock
    private GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    private WebTestClient webTestClient;

    private static final String EVENT_ID = "event-1";
    private static final Event EVENT = new Event(
            EVENT_ID, "Rock Concert",
            Instant.parse("2027-06-15T20:00:00Z"),
            "Movistar Arena", 500, 500, 0L);

    @BeforeEach
    void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        var requestValidator = new RequestValidator(factory.getValidator());
        var handler = new EventHandler(createEventUseCase, getEventAvailabilityUseCase, requestValidator);
        var router = new EventRouter();

        webTestClient = WebTestClient
                .bindToRouterFunction(router.eventRoutes(handler))
                .build();
    }

    @Test
    void createEvent_returns201WithEventOnSuccess() {
        when(createEventUseCase.execute(any(Event.class))).thenReturn(Mono.just(EVENT));

        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Rock Concert",
                          "date": "2027-06-15T20:00:00Z",
                          "venue": "Movistar Arena",
                          "totalCapacity": 500
                        }""")
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectBody(EventResponse.class)
                .value(resp -> {
                    assertThat(resp.id()).isEqualTo(EVENT_ID);
                    assertThat(resp.name()).isEqualTo("Rock Concert");
                    assertThat(resp.availableTickets()).isEqualTo(500);
                });
    }

    @Test
    void getAvailability_returns200WithEventOnSuccess() {
        when(getEventAvailabilityUseCase.execute(EVENT_ID)).thenReturn(Mono.just(EVENT));

        webTestClient.get()
                .uri("/api/v1/events/event-1/availability")
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventResponse.class)
                .value(resp -> assertThat(resp.id()).isEqualTo(EVENT_ID));
    }

    @Test
    void getAllEvents_returns200WithEventList() {
        var event2 = new Event("event-2", "Festival",
                Instant.parse("2027-09-01T18:00:00Z"), "Open Air", 2000, 1800, 0L);
        when(getEventAvailabilityUseCase.findAll()).thenReturn(Flux.just(EVENT, event2));

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(2);
    }

    @Test
    void createEvent_returns5xxWhenValidationFails() {
        // Without GlobalExceptionHandler, validation errors propagate as 500
        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "",
                          "date": null,
                          "venue": "",
                          "totalCapacity": 0
                        }""")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getAllEvents_returnsEmptyListWhenNoEvents() {
        when(getEventAvailabilityUseCase.findAll()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(0);
    }
}
