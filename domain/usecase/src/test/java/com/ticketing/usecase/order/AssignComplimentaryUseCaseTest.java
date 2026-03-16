package com.ticketing.usecase.order;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignComplimentaryUseCaseTest {

    @Mock
    private EventGateway eventGateway;

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private AssignComplimentaryUseCase useCase;

    private final String eventId = "event-1";
    private final String userId = "user-1";
    private final int quantity = 2;
    private final String idempotencyKey = "key-123";

    @Test
    void execute_successfulAssignment() {
        Event event = new Event(eventId, "Test Event", Instant.now(), "Venue", 100, 10, 1L);

        when(orderGateway.findByIdempotencyKey(anyString())).thenReturn(Mono.empty());
        when(eventGateway.findById(eventId)).thenReturn(Mono.just(event));
        when(eventGateway.updateAvailableTickets(anyString(), anyInt(), anyLong())).thenReturn(Mono.just(event));
        when(orderGateway.save(any(PurchaseOrder.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.execute(eventId, userId, quantity, idempotencyKey))
                .assertNext(order -> {
                    assert order.status() == OrderStatus.COMPLIMENTARY;
                    assert order.ticketStatus() == TicketStatus.COMPLIMENTARY;
                    assert order.quantity() == quantity;
                })
                .verifyComplete();

        verify(eventGateway).updateAvailableTickets(eventId, -quantity, 1L);
        verify(orderGateway).save(any(PurchaseOrder.class));
    }

    @Test
    void execute_returnsExistingOrder_whenIdempotencyKeyFound() {
        PurchaseOrder existing = new PurchaseOrder("order-1", eventId, userId, quantity, OrderStatus.COMPLIMENTARY, TicketStatus.COMPLIMENTARY, idempotencyKey, Instant.now(), Instant.now(), 0L);
        when(orderGateway.findByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(eventId, userId, quantity, idempotencyKey))
                .expectNext(existing)
                .verifyComplete();

        verify(eventGateway, never()).findById(anyString());
        verify(orderGateway, never()).save(any());
    }

    @Test
    void execute_fails_whenInsufficientInventory() {
        Event event = new Event(eventId, "Test Event", Instant.now(), "Venue", 100, 1, 1L);

        when(orderGateway.findByIdempotencyKey(idempotencyKey)).thenReturn(Mono.empty());
        when(eventGateway.findById(eventId)).thenReturn(Mono.just(event));

        StepVerifier.create(useCase.execute(eventId, userId, quantity, idempotencyKey))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).errorType() == BusinessErrorType.INSUFFICIENT_TICKETS)
                .verify();

        verify(orderGateway, never()).save(any());
    }
}
