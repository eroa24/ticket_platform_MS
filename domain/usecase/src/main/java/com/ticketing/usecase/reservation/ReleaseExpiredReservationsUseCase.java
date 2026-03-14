package com.ticketing.usecase.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ticketing.model.common.DomainConstants;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;

import reactor.core.publisher.Mono;

public class ReleaseExpiredReservationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredReservationsUseCase.class);

    private final OrderGateway orderGateway;
    private final EventGateway eventGateway;

    public ReleaseExpiredReservationsUseCase(OrderGateway orderGateway, EventGateway eventGateway) {
        this.orderGateway = orderGateway;
        this.eventGateway = eventGateway;
    }

    /**
     * Scans for expired reservations and releases them.
     *
     * Reactive flow:
     *   findExpiredReservations → for each: releaseTickets → markExpired
     *
     * @return empty Mono signaling completion
     */
    public Mono<Void> execute() {
        return orderGateway.findExpiredReservations(DomainConstants.RESERVATION_TIMEOUT_MINUTES)
                .flatMap(this::releaseAndExpire)
                .then();
    }

    private Mono<PurchaseOrder> releaseAndExpire(PurchaseOrder expiredOrder) {
        return releaseTickets(expiredOrder)
                .then(markAsExpired(expiredOrder))
                .doOnSuccess(order -> log.info("Released expired reservation: orderId={}, eventId={}, quantity={}",
                        order.id(), order.eventId(), order.quantity()))
                .onErrorResume(error -> handleReleaseError(expiredOrder, error));
    }

    private Mono<Void> releaseTickets(PurchaseOrder order) {
        return eventGateway.findById(order.eventId())
                .flatMap(event -> eventGateway.updateAvailableTickets(
                        event.id(), order.quantity(), event.version()))
                .then();
    }

    private Mono<PurchaseOrder> markAsExpired(PurchaseOrder order) {
        var expiredOrder = order.withStatus(OrderStatus.EXPIRED);
        return orderGateway.updateStatus(expiredOrder, order.version());
    }

    private Mono<PurchaseOrder> handleReleaseError(PurchaseOrder order, Throwable error) {
        log.error("Failed to release expired reservation: orderId={}, error={}",
                order.id(), error.getMessage());
        return Mono.empty();
    }
}
