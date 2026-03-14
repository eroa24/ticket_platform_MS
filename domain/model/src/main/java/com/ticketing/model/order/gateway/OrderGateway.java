package com.ticketing.model.order.gateway;

import com.ticketing.model.order.PurchaseOrder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderGateway {

    /**
     * @param order the order to save
     * @return the saved order
     */
    Mono<PurchaseOrder> save(PurchaseOrder order);

    /**
     * @param id the order id
     * @return the order if found, or empty Mono
     */
    Mono<PurchaseOrder> findById(String id);

    /**
     * @param idempotencyKey the client-provided idempotency key
     * @return the existing order if found, or empty Mono
     */
    Mono<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);

    /**
     * @param reservationTimeoutMinutes the maximum reservation duration in minutes
     * @return a Flux of expired orders
     */
    Flux<PurchaseOrder> findExpiredReservations(long reservationTimeoutMinutes);

    /**
     * @param order           the order with the new status
     * @param expectedVersion the expected version for optimistic locking
     * @return the updated order
     */
    Mono<PurchaseOrder> updateStatus(PurchaseOrder order, long expectedVersion);
}
