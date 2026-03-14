package com.ticketing.web.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.ticketing.web.handler.EventHandler;

@Configuration
public class EventRouter {

    @Bean
    public RouterFunction<ServerResponse> eventRoutes(EventHandler handler) {
        return RouterFunctions.route()
                .path("/api/v1/events", builder -> builder
                        .POST("", handler::createEvent)
                        .GET("", handler::getAllEvents)
                        .GET("/{id}/availability", handler::getAvailability))
                .build();
    }
}
