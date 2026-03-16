package com.ticketing.sqs.publisher;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;
import com.ticketing.sqs.dto.PurchaseMessage;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SQSOrderPublisherTest {

    @Mock
    private SqsTemplate sqsTemplate;

    private SQSOrderPublisher publisher;
    private final String queueName = "test-queue";

    @BeforeEach
    void setUp() {
        publisher = new SQSOrderPublisher(sqsTemplate, queueName);
    }

    @Test
    void sendPurchaseRequest_ShouldSuccessfullyPublishMessage() {
        PurchaseOrder order = new PurchaseOrder(
                "1", "e1", "u1", 2, OrderStatus.PENDING, TicketStatus.RESERVED,
                "key", Instant.now(), Instant.now(), 1L);

        when(sqsTemplate.sendAsync(eq(queueName), any(PurchaseMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(publisher.sendPurchaseRequest(order))
                .verifyComplete();

        verify(sqsTemplate).sendAsync(eq(queueName), any(PurchaseMessage.class));
    }

    @Test
    void sendPurchaseRequest_WhenError_ShouldPropagateError() {
        PurchaseOrder order = new PurchaseOrder(
                "1", "e1", "u1", 2, OrderStatus.PENDING, TicketStatus.RESERVED,
                "key", Instant.now(), Instant.now(), 1L);

        when(sqsTemplate.sendAsync(eq(queueName), any(PurchaseMessage.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS Error")));

        StepVerifier.create(publisher.sendPurchaseRequest(order))
                .expectError(RuntimeException.class)
                .verify();
    }
}
