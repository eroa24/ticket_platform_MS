package com.ticketing.usecase.order;

import java.util.Optional;
import java.util.UUID;

import com.ticketing.model.common.DomainConstants;
import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.order.gateway.OrderMessageGateway;

import reactor.core.publisher.Mono;

public class InitiatePurchaseUseCase {

    private final EventGateway eventGateway;
    private final OrderGateway orderGateway;
    private final OrderMessageGateway orderMessageGateway;

    public InitiatePurchaseUseCase(EventGateway eventGateway,
                                   OrderGateway orderGateway,
                                   OrderMessageGateway orderMessageGateway) {
        this.eventGateway = eventGateway;
        this.orderGateway = orderGateway;
        this.orderMessageGateway = orderMessageGateway;
    }

    /**
     * @param eventId        the event to purchase tickets for
     * @param userId         the buyer's identifier
     * @param quantity       the number of tickets to purchase
     * @param idempotencyKey client-provided key to prevent duplicates
     * @return the created purchase order with PENDING status
     */
    public Mono<PurchaseOrder> execute(String eventId, String userId,
                                       int quantity, String idempotencyKey) {
        return validateRequest(eventId, userId, quantity, idempotencyKey)
                .then(checkIdempotency(idempotencyKey))
                .switchIfEmpty(createNewOrder(eventId, userId, quantity, idempotencyKey));
    }

    private Mono<Void> validateRequest(String eventId, String userId,
                                       int quantity, String idempotencyKey) {
        return Optional.of(quantity)
                .filter(q -> q > 0 && q <= DomainConstants.MAX_TICKETS_PER_PURCHASE)
                .map(__ -> Mono.<Void>empty())
                .orElse(Mono.defer(() -> Mono.error(
                        BusinessErrorType.MAX_TICKETS_EXCEEDED.build(DomainConstants.MAX_TICKETS_PER_PURCHASE))))
                .then(validateNotBlank(eventId, "eventId"))
                .then(validateNotBlank(userId, "userId"))
                .then(validateNotBlank(idempotencyKey, "idempotencyKey"));
    }

    private Mono<Void> validateNotBlank(String value, String fieldName) {
        return Optional.ofNullable(value)
                .filter(v -> !v.isBlank())
                .map(__ -> Mono.<Void>empty())
                .orElse(Mono.defer(() -> Mono.error(
                        BusinessErrorType.INVALID_REQUEST.build(fieldName + " is required"))));
    }

    private Mono<PurchaseOrder> checkIdempotency(String idempotencyKey) {
        return orderGateway.findByIdempotencyKey(idempotencyKey);
    }

    private Mono<PurchaseOrder> createNewOrder(String eventId, String userId,
                                               int quantity, String idempotencyKey) {
        return fetchAndValidateEvent(eventId, quantity)
                .flatMap(event -> eventGateway.updateAvailableTickets(
                        event.id(), -quantity, event.version()))
                .then(buildAndSaveOrder(eventId, userId, quantity, idempotencyKey))
                .flatMap(this::enqueueAndReturn);
    }

    private Mono<Event> fetchAndValidateEvent(String eventId, int quantity) {
        return eventGateway.findById(eventId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.EVENT_NOT_FOUND.build(eventId))))
                .filter(event -> event.hasAvailableTickets(quantity))
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.INSUFFICIENT_TICKETS.build(
                                eventId, quantity, "check current availability"))));
    }

    private Mono<PurchaseOrder> buildAndSaveOrder(String eventId, String userId,
                                                  int quantity, String idempotencyKey) {
        var order = PurchaseOrder.create(
                UUID.randomUUID().toString(), eventId, userId, quantity, idempotencyKey);
        return orderGateway.save(order);
    }

    private Mono<PurchaseOrder> enqueueAndReturn(PurchaseOrder savedOrder) {
        return orderMessageGateway.sendPurchaseRequest(savedOrder)
                .thenReturn(savedOrder);
    }
}
