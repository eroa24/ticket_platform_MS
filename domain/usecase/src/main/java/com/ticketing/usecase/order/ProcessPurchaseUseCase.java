package com.ticketing.usecase.order;

import com.ticketing.model.common.DomainConstants;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;

import reactor.core.publisher.Mono;

public class ProcessPurchaseUseCase {

    private final EventGateway eventGateway;
    private final OrderGateway orderGateway;

    public ProcessPurchaseUseCase(EventGateway eventGateway, OrderGateway orderGateway) {
        this.eventGateway = eventGateway;
        this.orderGateway = orderGateway;
    }

    /**
     * Processes a purchase order received from the SQS queue.
     *
     * Flow:
     *   - Terminal orders (CONFIRMED, REJECTED, EXPIRED) are returned as-is (idempotent).
     *   - Expired reservations are released back to inventory and marked EXPIRED.
     *   - Active reservations are confirmed → tickets transition to SOLD.
     *
     * Note: ticket availability was already deducted atomically during reservation
     * (InitiatePurchaseUseCase). This step only confirms the final sale state.
     *
     * @param orderId the order to process
     * @return the processed order with updated status and ticketStatus
     */
    public Mono<PurchaseOrder> execute(String orderId) {
        return orderGateway.findById(orderId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.ORDER_NOT_FOUND.build(orderId))))
                .flatMap(this::processOrder);
    }

    private Mono<PurchaseOrder> processOrder(PurchaseOrder order) {
        return order.status().isTerminal()
                ? Mono.just(order)
                : order.isReservationExpired(DomainConstants.RESERVATION_TIMEOUT_MINUTES)
                        ? expireAndReleaseOrder(order)
                        : confirmOrder(order);
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
                .then(markAsExpired(order));
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
