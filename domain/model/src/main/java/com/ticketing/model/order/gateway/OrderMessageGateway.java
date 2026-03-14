package com.ticketing.model.order.gateway;

import com.ticketing.model.order.PurchaseOrder;

import reactor.core.publisher.Mono;

public interface OrderMessageGateway {

    /**
     * @param order the purchase order to enqueue
     * @return empty Mono upon successful publishing
     */
    Mono<Void> sendPurchaseRequest(PurchaseOrder order);
}
