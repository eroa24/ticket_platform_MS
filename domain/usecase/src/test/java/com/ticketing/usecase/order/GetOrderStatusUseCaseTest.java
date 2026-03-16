package com.ticketing.usecase.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetOrderStatusUseCaseTest {

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private GetOrderStatusUseCase useCase;

    private static final PurchaseOrder ORDER =
            PurchaseOrder.create("order-1", "event-1", "user-1", 2, "idem-1");

    @Test
    void execute_returnsOrderWhenFound() {
        when(orderGateway.findById("order-1")).thenReturn(Mono.just(ORDER));

        StepVerifier.create(useCase.execute("order-1"))
                .assertNext(o -> assertThat(o.id()).isEqualTo("order-1"))
                .verifyComplete();
    }

    @Test
    void execute_failsWithOrderNotFoundWhenMissing() {
        when(orderGateway.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("missing"))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.ORDER_NOT_FOUND);
                })
                .verify();
    }
}
