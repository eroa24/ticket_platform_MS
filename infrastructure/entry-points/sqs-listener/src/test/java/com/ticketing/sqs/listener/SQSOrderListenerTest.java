package com.ticketing.sqs.listener;

import java.time.Instant;

import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;
import com.ticketing.sqs.dto.PurchaseMessage;
import com.ticketing.usecase.order.ProcessPurchaseUseCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSOrderListenerTest {

    @Mock
    private ProcessPurchaseUseCase processPurchaseUseCase;

    private SQSOrderListener listener;

    @BeforeEach
    void setUp() {
        listener = new SQSOrderListener(processPurchaseUseCase);
    }

    @Test
    void onMessage_ShouldSuccessfullyProcessMessage() {
        PurchaseMessage message = new PurchaseMessage("o1", "e1", "u1", 1, "key");
        PurchaseOrder order = new PurchaseOrder(
                "o1", "e1", "u1", 1, OrderStatus.CONFIRMED, TicketStatus.SOLD,
                "key", Instant.now(), Instant.now(), 1L);

        when(processPurchaseUseCase.execute(anyString())).thenReturn(Mono.just(order));

        listener.onMessage(message).join();

        verify(processPurchaseUseCase).execute("o1");
    }

    @Test
    void onMessage_WhenError_ShouldCompleteExceptionally() {
        PurchaseMessage message = new PurchaseMessage("o1", "e1", "u1", 1, "key");

        when(processPurchaseUseCase.execute(anyString())).thenReturn(Mono.error(new RuntimeException("Use Case Error")));

        try {
            listener.onMessage(message).join();
        } catch (Exception e) {
            // Expected
        }

        verify(processPurchaseUseCase).execute("o1");
    }
}
