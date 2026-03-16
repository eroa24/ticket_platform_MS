package com.ticketing.usecase.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.order.gateway.OrderMessageGateway;
import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InitiatePurchaseUseCaseTest {

    @Mock
    private EventGateway eventGateway;
    @Mock
    private OrderGateway orderGateway;
    @Mock
    private OrderMessageGateway orderMessageGateway;

    private InitiatePurchaseUseCase useCase;

    private static final String EVENT_ID = "event-1";
    private static final String USER_ID = "user-1";
    private static final String IDEM_KEY = "idem-key-1";
    private static final int QUANTITY = 2;

    private static final Event EVENT = new Event(EVENT_ID, "Concert",
            Instant.parse("2027-01-01T00:00:00Z"), "Venue", 100, 50, 0L);

    @BeforeEach
    void setUp() {
        useCase = new InitiatePurchaseUseCase(eventGateway, orderGateway, orderMessageGateway);

        lenient().when(orderGateway.findByIdempotencyKey(anyString())).thenReturn(Mono.empty());
        lenient().when(eventGateway.findById(anyString())).thenReturn(Mono.empty());
        lenient().when(eventGateway.updateAvailableTickets(anyString(), anyInt(), anyLong()))
                .thenReturn(Mono.just(EVENT));
        lenient().when(orderGateway.save(any()))
                .thenAnswer(inv -> Mono.justOrEmpty((PurchaseOrder) inv.getArgument(0)));
        lenient().when(orderMessageGateway.sendPurchaseRequest(any())).thenReturn(Mono.empty());
    }

    @Test
    void execute_createsNewOrderAndEnqueues() {
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.empty());
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(eq(EVENT_ID), eq(-QUANTITY), eq(0L)))
                .thenReturn(Mono.just(EVENT.withReducedAvailability(QUANTITY)));
        when(orderGateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderMessageGateway.sendPurchaseRequest(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, QUANTITY, IDEM_KEY))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.ticketStatus()).isEqualTo(TicketStatus.RESERVED);
                    assertThat(order.eventId()).isEqualTo(EVENT_ID);
                    assertThat(order.quantity()).isEqualTo(QUANTITY);
                    assertThat(order.id()).isNotBlank();
                })
                .verifyComplete();

        verify(eventGateway).updateAvailableTickets(EVENT_ID, -QUANTITY, 0L);
        verify(orderGateway).save(any());
        verify(orderMessageGateway).sendPurchaseRequest(any());
    }

    @Test
    void execute_returnsExistingOrderOnDuplicateIdempotencyKey() {
        var existingOrder = PurchaseOrder.create("existing-id", EVENT_ID, USER_ID, QUANTITY, IDEM_KEY);
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.just(existingOrder));

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, QUANTITY, IDEM_KEY))
                .assertNext(order -> assertThat(order.id()).isEqualTo("existing-id"))
                .verifyComplete();

        verify(eventGateway, never()).updateAvailableTickets(anyString(), anyInt(), anyLong());
        verify(orderGateway, never()).save(any());
    }

    @Test
    void execute_failsWhenEventNotFound() {
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.empty());
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, QUANTITY, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.EVENT_NOT_FOUND);
                })
                .verify();
    }

    @Test
    void execute_failsWhenInsufficientTickets() {
        var soldOutEvent = new Event(EVENT_ID, "Concert",
                Instant.parse("2027-01-01T00:00:00Z"), "Venue", 100, 1, 0L);
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.empty());
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(soldOutEvent));

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, 5, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INSUFFICIENT_TICKETS);
                })
                .verify();
    }

    @Test
    void execute_failsWhenQuantityExceedsMaximum() {
        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, 11, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.MAX_TICKETS_EXCEEDED);
                })
                .verify();
    }

    @Test
    void execute_failsWhenQuantityIsZero() {
        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, 0, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.MAX_TICKETS_EXCEEDED);
                })
                .verify();
    }

    @Test
    void execute_failsWhenEventIdIsBlank() {
        StepVerifier.create(useCase.execute("  ", USER_ID, QUANTITY, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }

    @Test
    void execute_failsWhenUserIdIsNull() {
        StepVerifier.create(useCase.execute(EVENT_ID, null, QUANTITY, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }

    @Test
    void execute_retriesOnOptimisticLockFailureAndSucceedsOnSecondAttempt() {
        var optimisticLockError = BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build();
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.empty());
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(eq(EVENT_ID), eq(-QUANTITY), anyLong()))
                .thenReturn(Mono.error(optimisticLockError),
                        Mono.just(EVENT.withReducedAvailability(QUANTITY)));
        when(orderGateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderMessageGateway.sendPurchaseRequest(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, QUANTITY, IDEM_KEY))
                .assertNext(order -> assertThat(order.status()).isEqualTo(OrderStatus.PENDING))
                .verifyComplete();
    }

    @Test
    void execute_failsAfterExhaustingOptimisticLockRetries() {
        var optimisticLockError = BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build();
        when(orderGateway.findByIdempotencyKey(IDEM_KEY)).thenReturn(Mono.empty());
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(anyString(), anyInt(), anyLong()))
                .thenReturn(Mono.error(optimisticLockError));

        StepVerifier.create(useCase.execute(EVENT_ID, USER_ID, QUANTITY, IDEM_KEY))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.OPTIMISTIC_LOCK_FAILURE);
                })
                .verify();
    }
}
