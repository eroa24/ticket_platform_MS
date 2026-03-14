package com.ticketing.web.handler;

import com.ticketing.usecase.order.GetOrderStatusUseCase;
import com.ticketing.usecase.order.InitiatePurchaseUseCase;
import com.ticketing.web.dto.PurchaseRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class OrderHandler {

    private final InitiatePurchaseUseCase initiatePurchaseUseCase;
    private final GetOrderStatusUseCase getOrderStatusUseCase;

    public OrderHandler(InitiatePurchaseUseCase initiatePurchaseUseCase,
                        GetOrderStatusUseCase getOrderStatusUseCase) {
        this.initiatePurchaseUseCase = initiatePurchaseUseCase;
        this.getOrderStatusUseCase = getOrderStatusUseCase;
    }

    public Mono<ServerResponse> initiatePurchase(ServerRequest request) {
        return request.bodyToMono(PurchaseRequest.class)
                .flatMap(req -> initiatePurchaseUseCase.execute(
                        req.eventId(), req.userId(), req.quantity(), req.idempotencyKey()))
                .flatMap(order -> ServerResponse.status(202).bodyValue(order));
    }

    public Mono<ServerResponse> getOrderStatus(ServerRequest request) {
        var orderId = request.pathVariable("id");
        return getOrderStatusUseCase.execute(orderId)
                .flatMap(order -> ServerResponse.ok().bodyValue(order));
    }
}
