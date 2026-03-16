package com.ticketing.usecase.order;

import com.ticketing.model.common.DomainConstants;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

public class ProcessPurchaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPurchaseUseCase.class);

    private final EventGateway eventGateway;
    private final OrderGateway orderGateway;

    public ProcessPurchaseUseCase(EventGateway eventGateway, OrderGateway orderGateway) {
        this.eventGateway = eventGateway;
        this.orderGateway = orderGateway;
    }

    /**
     * Processes a purchase order received from the SQS queue.
     *
     * @param orderId the order to process
     * @return the processed order with updated status and ticketStatus
     */
    public Mono<PurchaseOrder> execute(String orderId) {
        return orderGateway.findById(orderId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.ORDER_NOT_FOUND.build(orderId))))
                .flatMap(this::processOrder)
                .doOnSubscribe(s -> log.info("Processing order: orderId={}", orderId))
                .doOnSuccess(o -> log.info("Order processed: orderId={}, status={}, ticketStatus={}",
                        o.id(), o.status(), o.ticketStatus()))
                .doOnError(e -> log.error("Order processing failed: orderId={}, error={}", orderId, e.getMessage()));
    }

    private Mono<PurchaseOrder> processOrder(PurchaseOrder order) {
        return Mono.just(order)
                .filter(o -> !o.status().isTerminal())
                .flatMap(this::determineOrderFate)
                .switchIfEmpty(Mono.just(order));
    }

    private Mono<PurchaseOrder> determineOrderFate(PurchaseOrder order) {
        return order.isReservationExpired(DomainConstants.RESERVATION_TIMEOUT_MINUTES)
                ? validateAndExpire(order)
                : validateAndConfirm(order);
    }

    private Mono<PurchaseOrder> validateAndConfirm(PurchaseOrder order) {
        return validateTransition(order, OrderStatus.CONFIRMED)
                .flatMap(validOrder -> confirmOrder(validOrder));
    }

    private Mono<PurchaseOrder> validateAndExpire(PurchaseOrder order) {
        return validateTransition(order, OrderStatus.EXPIRED)
                .flatMap(validOrder -> expireAndReleaseOrder(validOrder));
    }

    private Mono<PurchaseOrder> validateTransition(PurchaseOrder order, OrderStatus nextStatus) {
        var nextTicketStatus = PurchaseOrder.resolveTicketStatus(nextStatus);
        return order.ticketStatus().canTransitionTo(nextTicketStatus)
                ? Mono.just(order)
                : Mono.defer(() -> Mono.error(BusinessErrorType.INVALID_STATE_TRANSITION
                        .build(order.ticketStatus(), nextTicketStatus)));
    }

    private Mono<PurchaseOrder> confirmOrder(PurchaseOrder order) {
        var confirmedOrder = order.withStatus(OrderStatus.CONFIRMED);
        return orderGateway.updateStatus(confirmedOrder, order.version());
    }

    /**
     * Releases reserved tickets back to the event inventory and marks the order as EXPIRED.
     * Uses a single findById call (no duplication) and propagates errors naturally.
     */
    private Mono<PurchaseOrder> expireAndReleaseOrder(PurchaseOrder order) {
        return releaseReservedTickets(order)
                .then(Mono.defer(() -> markAsExpired(order)));
    }

    private Mono<Void> releaseReservedTickets(PurchaseOrder order) {
        return eventGateway.findById(order.eventId())
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.EVENT_NOT_FOUND.build(order.eventId()))))
                .flatMap(event -> eventGateway.updateAvailableTickets(
                        event.id(), order.quantity(), event.version()))
                .then();
    }

    private Mono<PurchaseOrder> markAsExpired(PurchaseOrder order) {
        var expiredOrder = order.withStatus(OrderStatus.EXPIRED);
        return orderGateway.updateStatus(expiredOrder, order.version());
    }
}
