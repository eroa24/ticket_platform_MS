package com.ticketing.usecase.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProcessPurchaseUseCaseTest {

    @Mock
    private EventGateway eventGateway;
    @Mock
    private OrderGateway orderGateway;

    private ProcessPurchaseUseCase useCase;

    private static final String ORDER_ID = "order-1";
    private static final String EVENT_ID = "event-1";
    private static final String USER_ID = "user-1";
    private static final String IDEMPOTENCY_KEY = "idem-key-1";
    private static final Event EVENT = new Event(EVENT_ID, "Concert",
            Instant.parse("2027-01-01T00:00:00Z"), "Venue", 100, 48, 1L);

    @BeforeEach
    void setUp() {
        useCase = new ProcessPurchaseUseCase(eventGateway, orderGateway);

        lenient().when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.justOrEmpty((PurchaseOrder) inv.getArgument(0)));
    }

    @Test
    void execute_confirmsActiveReservation() {
        var pendingOrder = PurchaseOrder.create(ORDER_ID, EVENT_ID, USER_ID, 2, IDEMPOTENCY_KEY);
        when(orderGateway.findById(ORDER_ID)).thenReturn(Mono.just(pendingOrder));
        when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(ORDER_ID))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
                    assertThat(order.ticketStatus()).isEqualTo(TicketStatus.SOLD);
                })
                .verifyComplete();

        verify(eventGateway, never()).findById(any());
    }

    @Test
    void execute_expiresAndReleasesExpiredReservation() {
        var expiredOrder = new PurchaseOrder(
                ORDER_ID, EVENT_ID, USER_ID, 2,
                OrderStatus.PENDING, TicketStatus.RESERVED,
                IDEMPOTENCY_KEY,
                Instant.now().minus(11, ChronoUnit.MINUTES),
                Instant.now(), 0L);

        when(orderGateway.findById(ORDER_ID)).thenReturn(Mono.just(expiredOrder));
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(eq(EVENT_ID), eq(2), eq(1L)))
                .thenReturn(Mono.just(EVENT.withIncreasedAvailability(2)));
        when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(ORDER_ID))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.EXPIRED);
                    assertThat(order.ticketStatus()).isEqualTo(TicketStatus.AVAILABLE);
                })
                .verifyComplete();

        verify(eventGateway).findById(EVENT_ID);
        verify(eventGateway).updateAvailableTickets(EVENT_ID, 2, 1L);
    }

    @Test
    void execute_returnsTerminalOrderWithoutChanges() {
        var confirmedOrder = new PurchaseOrder(
                ORDER_ID, EVENT_ID, USER_ID, 2,
                OrderStatus.CONFIRMED, TicketStatus.SOLD,
                IDEMPOTENCY_KEY,
                Instant.now().minus(15, ChronoUnit.MINUTES),
                Instant.now(), 1L);

        when(orderGateway.findById(ORDER_ID)).thenReturn(Mono.just(confirmedOrder));

        StepVerifier.create(useCase.execute(ORDER_ID))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
                    assertThat(order.ticketStatus()).isEqualTo(TicketStatus.SOLD);
                })
                .verifyComplete();

        verify(orderGateway, never()).updateStatus(any(), anyLong());
        verify(eventGateway, never()).findById(any());
    }

    @Test
    void execute_failsWhenOrderNotFound() {
        when(orderGateway.findById(ORDER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.ORDER_NOT_FOUND);
                })
                .verify();
    }

    @Test
    void execute_failsWhenEventNotFoundDuringExpiration() {
        var expiredOrder = new PurchaseOrder(
                ORDER_ID, EVENT_ID, USER_ID, 2,
                OrderStatus.PENDING, TicketStatus.RESERVED,
                IDEMPOTENCY_KEY,
                Instant.now().minus(11, ChronoUnit.MINUTES),
                Instant.now(), 0L);

        when(orderGateway.findById(ORDER_ID)).thenReturn(Mono.just(expiredOrder));
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.EVENT_NOT_FOUND);
                })
                .verify();
    }
}
