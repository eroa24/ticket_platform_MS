package com.ticketing.web.router;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.web.handler.OrderHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OrderRouterTest {

    @Mock
    private OrderHandler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        var router = new OrderRouter();
        var routerFunction = router.orderRoutes(handler);
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    void route_purchase() {
        when(handler.initiatePurchase(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.post()
                .uri("/api/v1/orders/purchase")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void route_complimentary() {
        when(handler.assignComplimentary(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.post()
                .uri("/api/v1/orders/complimentary")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void route_getOrder() {
        when(handler.getOrderStatus(any())).thenReturn(ServerResponse.ok().build());

        webTestClient.get()
                .uri("/api/v1/orders/123")
                .exchange()
                .expectStatus().isOk();
    }
}
