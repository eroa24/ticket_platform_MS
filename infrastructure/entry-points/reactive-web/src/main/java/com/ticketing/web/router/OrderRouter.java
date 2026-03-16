package com.ticketing.web.router;

import com.ticketing.web.handler.OrderHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class OrderRouter {

    @Bean
    public RouterFunction<ServerResponse> orderRoutes(OrderHandler handler) {
        return RouterFunctions.route()
                .path("/api/v1/orders", builder -> builder
                        .POST("/purchase", handler::initiatePurchase)
                        .POST("/complimentary", handler::assignComplimentary)
                        .GET("/{id}", handler::getOrderStatus))
                .build();
    }
}
