package com.ticketing.usecase.order;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.ticketing.model.common.DomainConstants;
import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.order.gateway.OrderMessageGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class InitiatePurchaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiatePurchaseUseCase.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_INITIAL_BACKOFF = Duration.ofMillis(100);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(1);

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
     * Initiates a ticket purchase by reserving availability and enqueuing for async processing.
     *
     * Retries automatically on optimistic lock conflicts (concurrent purchase attempts).
     * Idempotency: returns the existing order if the idempotencyKey was already processed.
     *
     * @param eventId        the event to purchase tickets for
     * @param userId         the buyer's identifier
     * @param quantity       the number of tickets to purchase (1 to MAX_TICKETS_PER_PURCHASE)
     * @param idempotencyKey client-provided key to prevent duplicate orders
     * @return the created (or existing) purchase order with PENDING status / RESERVED tickets
     */
    public Mono<PurchaseOrder> execute(String eventId, String userId,
                                       int quantity, String idempotencyKey) {
        return validateRequest(eventId, userId, quantity, idempotencyKey)
                .then(checkIdempotency(idempotencyKey))
                .switchIfEmpty(createNewOrder(eventId, userId, quantity, idempotencyKey)
                        .retryWhen(retryOnOptimisticLockFailure()))
                .doOnSubscribe(s -> log.info("Purchase initiated: eventId={}, quantity={}", eventId, quantity))
                .doOnSuccess(order -> log.info("Purchase reserved: orderId={}, eventId={}, status={}, ticketStatus={}",
                        order.id(), order.eventId(), order.status(), order.ticketStatus()))
                .doOnError(e -> log.error("Purchase failed: eventId={}, error={}", eventId, e.getMessage()));
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

    /**
     * Retry strategy for concurrent write conflicts on DynamoDB.
     * Uses exponential backoff to reduce contention under high load.
     * Only retries on OPTIMISTIC_LOCK_FAILURE — all other errors propagate immediately.
     */
    private Retry retryOnOptimisticLockFailure() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_INITIAL_BACKOFF)
                .maxBackoff(RETRY_MAX_BACKOFF)
                .filter(e -> e instanceof BusinessException be
                        && be.errorType() == BusinessErrorType.OPTIMISTIC_LOCK_FAILURE)
                .onRetryExhaustedThrow((spec, signal) ->
                        BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build());
    }
}
