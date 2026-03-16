package com.ticketing.web.router;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.web.handler.EventHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EventRouterTest {

    @Mock
    private EventHandler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var router = new EventRouter();
        var routerFunction = router.eventRoutes(handler);
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    void route_createEvent() {
        when(handler.createEvent(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.post()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void route_getAllEvents() {
        when(handler.getAllEvents(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void route_getAvailability() {
        when(handler.getAvailability(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.get()
                .uri("/api/v1/events/123/availability")
                .exchange()
                .expectStatus().isOk();
    }
}
