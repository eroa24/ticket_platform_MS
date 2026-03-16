package com.ticketing.usecase.order;

import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.ticket.TicketStatus;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for assigning complimentary (free) tickets.
 * Unlike regular purchases, these are immediate and final.
 */
@RequiredArgsConstructor
public class AssignComplimentaryUseCase {

    private final EventGateway eventGateway;
    private final OrderGateway orderGateway;

    public Mono<PurchaseOrder> execute(String eventId, String userId, int quantity, String idempotencyKey) {
        return orderGateway.findByIdempotencyKey(idempotencyKey)
                .switchIfEmpty(Mono.defer(() -> createComplimentaryOrder(eventId, userId, quantity, idempotencyKey)));
    }

    private Mono<PurchaseOrder> createComplimentaryOrder(String eventId, String userId, int quantity, String idempotencyKey) {
        return eventGateway.findById(eventId)
                .switchIfEmpty(Mono.error(BusinessErrorType.EVENT_NOT_FOUND.build(eventId)))
                .flatMap(event -> {
                    if (event.availableTickets() < quantity) {
                        return Mono.error(BusinessErrorType.INSUFFICIENT_TICKETS.build(eventId, quantity, event.availableTickets()));
                    }
                    return eventGateway.updateAvailableTickets(eventId, -quantity, event.version())
                            .map(updatedEvent -> new PurchaseOrder(
                                    UUID.randomUUID().toString(),
                                    eventId,
                                    userId,
                                    quantity,
                                    OrderStatus.COMPLIMENTARY,
                                    TicketStatus.COMPLIMENTARY,
                                    idempotencyKey,
                                    Instant.now(),
                                    Instant.now(),
                                    0L
                            ))
                            .flatMap(orderGateway::save);
                });
    }
}
