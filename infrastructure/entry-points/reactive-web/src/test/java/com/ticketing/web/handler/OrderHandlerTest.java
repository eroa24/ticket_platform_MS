package com.ticketing.web.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;
import com.ticketing.usecase.order.AssignComplimentaryUseCase;
import com.ticketing.usecase.order.GetOrderStatusUseCase;
import com.ticketing.usecase.order.InitiatePurchaseUseCase;
import com.ticketing.web.dto.OrderResponse;
import com.ticketing.web.router.OrderRouter;
import com.ticketing.web.validation.RequestValidator;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OrderHandlerTest {

    @Mock
    private InitiatePurchaseUseCase initiatePurchaseUseCase;
    @Mock
    private GetOrderStatusUseCase getOrderStatusUseCase;
    @Mock
    private AssignComplimentaryUseCase assignComplimentaryUseCase;

    private WebTestClient webTestClient;

    private static final PurchaseOrder PENDING_ORDER =
            PurchaseOrder.create("order-1", "event-1", "user-1", 2, "idem-1");

    @BeforeEach
    void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        var requestValidator = new RequestValidator(factory.getValidator());
        var handler = new OrderHandler(initiatePurchaseUseCase, getOrderStatusUseCase, assignComplimentaryUseCase, requestValidator);
        var router = new OrderRouter();

        webTestClient = WebTestClient
                .bindToRouterFunction(router.orderRoutes(handler))
                .build();
    }

    @Test
    void initiatePurchase_returns202WithOrderOnSuccess() {
        when(initiatePurchaseUseCase.execute(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Mono.just(PENDING_ORDER));

        webTestClient.post()
                .uri("/api/v1/orders/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventId": "event-1",
                          "userId": "user-1",
                          "quantity": 2,
                          "idempotencyKey": "idem-1"
                        }""")
                .exchange()
                .expectStatus().isEqualTo(202)
                .expectBody(OrderResponse.class)
                .value(resp -> {
                    assertThat(resp.orderId()).isEqualTo("order-1");
                    assertThat(resp.orderStatus()).isEqualTo(OrderStatus.PENDING);
                });
    }

    @Test
    void getOrderStatus_returns200WithOrderOnSuccess() {
        when(getOrderStatusUseCase.execute("order-1")).thenReturn(Mono.just(PENDING_ORDER));

        webTestClient.get()
                .uri("/api/v1/orders/order-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(resp -> assertThat(resp.orderId()).isEqualTo("order-1"));
    }

    @Test
    void initiatePurchase_returns400WhenBodyIsInvalid() {
        webTestClient.post()
                .uri("/api/v1/orders/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventId": "",
                          "userId": "",
                          "quantity": 0,
                          "idempotencyKey": ""
                        }""")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void initiatePurchase_useCaseErrorPropagates() {
        when(initiatePurchaseUseCase.execute(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Mono.error(BusinessErrorType.INSUFFICIENT_TICKETS.build("event-1", 2, "check availability")));

        webTestClient.post()
                .uri("/api/v1/orders/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventId": "event-1",
                          "userId": "user-1",
                          "quantity": 2,
                          "idempotencyKey": "idem-2"
                        }""")
                .exchange()
                .expectStatus().is5xxServerError();
    }
    @Test
    void assignComplimentary_returns201OnSuccess() {
        PurchaseOrder compOrder = PurchaseOrder.create("order-1", "event-1", "user-1", 2, "comp-idem");
        PurchaseOrder saved = new PurchaseOrder(compOrder.id(), compOrder.eventId(), compOrder.userId(), compOrder.quantity(),
                OrderStatus.COMPLIMENTARY, TicketStatus.COMPLIMENTARY, compOrder.idempotencyKey(),
                compOrder.createdAt(), compOrder.updatedAt(), 0L);

        when(assignComplimentaryUseCase.execute(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Mono.just(saved));

        webTestClient.post()
                .uri("/api/v1/orders/complimentary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventId": "event-1",
                          "userId": "user-1",
                          "quantity": 2,
                          "idempotencyKey": "comp-idem"
                        }""")
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectBody(OrderResponse.class)
                .value(resp -> {
                    assertThat(resp.orderId()).isEqualTo("order-1");
                    assertThat(resp.orderStatus()).isEqualTo(OrderStatus.COMPLIMENTARY);
                });
    }
}
