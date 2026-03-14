package com.ticketing.usecase.order;

import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;

import reactor.core.publisher.Mono;

public class GetOrderStatusUseCase {

    private final OrderGateway orderGateway;

    public GetOrderStatusUseCase(OrderGateway orderGateway) {
        this.orderGateway = orderGateway;
    }

    /**
     * @param orderId the order identifier
     * @return the purchase order with its current status
     */
    public Mono<PurchaseOrder> execute(String orderId) {
        return orderGateway.findById(orderId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(BusinessErrorType.ORDER_NOT_FOUND.build(orderId))));
    }
}
