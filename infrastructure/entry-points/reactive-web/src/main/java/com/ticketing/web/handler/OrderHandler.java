package com.ticketing.web.handler;

import com.ticketing.usecase.order.AssignComplimentaryUseCase;
import com.ticketing.usecase.order.GetOrderStatusUseCase;
import com.ticketing.usecase.order.InitiatePurchaseUseCase;
import com.ticketing.web.dto.ComplimentaryRequest;
import com.ticketing.web.dto.OrderResponse;
import com.ticketing.web.dto.PurchaseRequest;
import com.ticketing.web.validation.RequestValidator;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class OrderHandler {

    private final InitiatePurchaseUseCase initiatePurchaseUseCase;
    private final GetOrderStatusUseCase getOrderStatusUseCase;
    private final AssignComplimentaryUseCase assignComplimentaryUseCase;
    private final RequestValidator validator;

    public OrderHandler(InitiatePurchaseUseCase initiatePurchaseUseCase,
                        GetOrderStatusUseCase getOrderStatusUseCase,
                        AssignComplimentaryUseCase assignComplimentaryUseCase,
                        RequestValidator validator) {
        this.initiatePurchaseUseCase = initiatePurchaseUseCase;
        this.getOrderStatusUseCase = getOrderStatusUseCase;
        this.assignComplimentaryUseCase = assignComplimentaryUseCase;
        this.validator = validator;
    }

    public Mono<ServerResponse> initiatePurchase(ServerRequest request) {
        return request.bodyToMono(PurchaseRequest.class)
                .flatMap(validator::validate)
                .flatMap(req -> initiatePurchaseUseCase.execute(
                        req.eventId(), req.userId(), req.quantity(), req.idempotencyKey()))
                .map(OrderResponse::from)
                .flatMap(order -> ServerResponse.status(202).bodyValue(order));
    }

    public Mono<ServerResponse> getOrderStatus(ServerRequest request) {
        var orderId = request.pathVariable("id");
        return getOrderStatusUseCase.execute(orderId)
                .map(OrderResponse::from)
                .flatMap(order -> ServerResponse.ok().bodyValue(order));
    }

    public Mono<ServerResponse> assignComplimentary(ServerRequest request) {
        return request.bodyToMono(ComplimentaryRequest.class)
                .flatMap(validator::validate)
                .flatMap(req -> assignComplimentaryUseCase.execute(
                        req.eventId(), req.userId(), req.quantity(), req.idempotencyKey()))
                .map(OrderResponse::from)
                .flatMap(order -> ServerResponse.status(201).bodyValue(order));
    }
}
