package com.ticketing.sqs.listener;

import java.util.concurrent.CompletableFuture;

import com.ticketing.sqs.dto.PurchaseMessage;
import com.ticketing.usecase.order.ProcessPurchaseUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.awspring.cloud.sqs.annotation.SqsListener;

@Component
public class SQSOrderListener {

    private static final Logger log = LoggerFactory.getLogger(SQSOrderListener.class);

    private final ProcessPurchaseUseCase processPurchaseUseCase;

    public SQSOrderListener(ProcessPurchaseUseCase processPurchaseUseCase) {
        this.processPurchaseUseCase = processPurchaseUseCase;
    }

    /**
     * Processes an incoming SQS purchase message asynchronously.
     *
     * Returns CompletableFuture<Void> — Spring Cloud AWS 4.x uses this for async acknowledgment:
     * the message is deleted from the queue only after the future completes successfully.
     * On failure, the message remains visible and retried (up to maxReceiveCount → DLQ).
     *
     * No .block() needed: toFuture() bridges Reactor to CompletableFuture non-blocking.
     */
    @SqsListener("${app.sqs.purchase-queue}")
    public CompletableFuture<Void> onMessage(PurchaseMessage message) {
        log.info("Received purchase message: orderId={}, eventId={}",
                message.orderId(), message.eventId());

        return processPurchaseUseCase.execute(message.orderId())
                .doOnSuccess(order -> log.info("Processed order: orderId={}, status={}, ticketStatus={}",
                        order.id(), order.status(), order.ticketStatus()))
                .doOnError(e -> log.error("Error processing order: orderId={}, error={}",
                        message.orderId(), e.getMessage()))
                .then()
                .toFuture();
    }
}
