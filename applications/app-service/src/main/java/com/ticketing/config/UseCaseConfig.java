package com.ticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.order.gateway.OrderMessageGateway;
import com.ticketing.usecase.event.CreateEventUseCase;
import com.ticketing.usecase.event.GetEventAvailabilityUseCase;
import com.ticketing.usecase.order.AssignComplimentaryUseCase;
import com.ticketing.usecase.order.GetOrderStatusUseCase;
import com.ticketing.usecase.order.InitiatePurchaseUseCase;
import com.ticketing.usecase.order.ProcessPurchaseUseCase;
import com.ticketing.usecase.reservation.ReleaseExpiredReservationsUseCase;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateEventUseCase createEventUseCase(EventGateway eventGateway) {
        return new CreateEventUseCase(eventGateway);
    }

    @Bean
    public GetEventAvailabilityUseCase getEventAvailabilityUseCase(EventGateway eventGateway) {
        return new GetEventAvailabilityUseCase(eventGateway);
    }

    @Bean
    public InitiatePurchaseUseCase initiatePurchaseUseCase(EventGateway eventGateway,
                                                           OrderGateway orderGateway,
                                                           OrderMessageGateway orderMessageGateway) {
        return new InitiatePurchaseUseCase(eventGateway, orderGateway, orderMessageGateway);
    }

    @Bean
    public ProcessPurchaseUseCase processPurchaseUseCase(EventGateway eventGateway,
                                                         OrderGateway orderGateway) {
        return new ProcessPurchaseUseCase(eventGateway, orderGateway);
    }

    @Bean
    public GetOrderStatusUseCase getOrderStatusUseCase(OrderGateway orderGateway) {
        return new GetOrderStatusUseCase(orderGateway);
    }

    @Bean
    public ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase(
            OrderGateway orderGateway, EventGateway eventGateway) {
        return new ReleaseExpiredReservationsUseCase(orderGateway, eventGateway);
    }

    @Bean
    public AssignComplimentaryUseCase assignComplimentaryUseCase(EventGateway eventGateway,
                                                                 OrderGateway orderGateway) {
        return new AssignComplimentaryUseCase(eventGateway, orderGateway);
    }
}
