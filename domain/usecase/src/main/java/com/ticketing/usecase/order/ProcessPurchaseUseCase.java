package com.ticketing.usecase.order;

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
     * @param orderId the order to process
     * @return the processed order with updated status
     */
    public Mono<PurchaseOrder> execute(String orderId) {
        return orderGateway.findById(orderId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.ORDER_NOT_FOUND.build(orderId))))
                .flatMap(this::processIfPending);
    }

    private Mono<PurchaseOrder> processIfPending(PurchaseOrder order) {
        return order.status().isTerminal()
                ? Mono.just(order)
                : attemptConfirmation(order);
    }

    private Mono<PurchaseOrder> attemptConfirmation(PurchaseOrder order) {
        return eventGateway.findById(order.eventId())
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.EVENT_NOT_FOUND.build(order.eventId()))))
                .flatMap(event -> event.hasAvailableTickets(order.quantity())
                        ? confirmOrder(order)
                        : rejectOrder(order));
    }

    private Mono<PurchaseOrder> confirmOrder(PurchaseOrder order) {
        var confirmedOrder = order.withStatus(OrderStatus.CONFIRMED);
        return orderGateway.updateStatus(confirmedOrder, order.version());
    }

    private Mono<PurchaseOrder> rejectOrder(PurchaseOrder order) {
        return releaseReservedTickets(order)
                .then(saveRejectedOrder(order));
    }

    private Mono<Void> releaseReservedTickets(PurchaseOrder order) {
        return eventGateway.findById(order.eventId())
                .flatMap(event -> eventGateway.updateAvailableTickets(
                        event.id(), order.quantity(), event.version()))
                .then();
    }

    private Mono<PurchaseOrder> saveRejectedOrder(PurchaseOrder order) {
        var rejectedOrder = order.withStatus(OrderStatus.REJECTED);
        return orderGateway.updateStatus(rejectedOrder, order.version());
    }
}
