package com.ticketing.usecase.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReleaseExpiredReservationsUseCaseTest {

    @Mock
    private OrderGateway orderGateway;
    @Mock
    private EventGateway eventGateway;

    private ReleaseExpiredReservationsUseCase useCase;

    private static final String EVENT_ID = "event-1";
    private static final Event EVENT = new Event(EVENT_ID, "Concert",
            Instant.parse("2027-01-01T00:00:00Z"), "Venue", 100, 48, 1L);

    @BeforeEach
    void setUp() {
        useCase = new ReleaseExpiredReservationsUseCase(orderGateway, eventGateway);

        lenient().when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.justOrEmpty((PurchaseOrder) inv.getArgument(0)));
    }

    private PurchaseOrder expiredOrder(String id) {
        return new PurchaseOrder(
                id, EVENT_ID, "user-1", 2,
                OrderStatus.PENDING, TicketStatus.RESERVED,
                "idem-" + id,
                Instant.now().minus(11, ChronoUnit.MINUTES),
                Instant.now(), 0L);
    }

    @Test
    void execute_completesWhenNoExpiredReservations() {
        when(orderGateway.findExpiredReservations(anyLong())).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(eventGateway, never()).findById(any());
    }

    @Test
    void execute_releasesAndExpiresEachExpiredOrder() {
        var order1 = expiredOrder("order-1");
        var order2 = expiredOrder("order-2");

        when(orderGateway.findExpiredReservations(anyLong())).thenReturn(Flux.just(order1, order2));
        when(eventGateway.findById(EVENT_ID)).thenReturn(Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(eq(EVENT_ID), eq(2), eq(1L)))
                .thenReturn(Mono.just(EVENT.withIncreasedAvailability(2)));
        when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(eventGateway, atLeastOnce()).findById(EVENT_ID);
        verify(orderGateway, atLeastOnce()).updateStatus(any(), anyLong());
    }

    @Test
    void execute_continuesProcessingWhenOneOrderFails() {
        var failingOrder = expiredOrder("order-fail");
        var goodOrder = expiredOrder("order-ok");

        when(orderGateway.findExpiredReservations(anyLong())).thenReturn(Flux.just(failingOrder, goodOrder));
        when(eventGateway.findById(EVENT_ID))
                .thenReturn(Mono.error(new RuntimeException("DynamoDB unavailable")),
                        Mono.just(EVENT));
        when(eventGateway.updateAvailableTickets(eq(EVENT_ID), eq(2), eq(1L)))
                .thenReturn(Mono.just(EVENT.withIncreasedAvailability(2)));
        when(orderGateway.updateStatus(any(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(eventGateway, atLeastOnce()).updateAvailableTickets(EVENT_ID, 2, 1L);
    }
}
