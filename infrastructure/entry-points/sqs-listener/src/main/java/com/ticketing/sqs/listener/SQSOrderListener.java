package com.ticketing.sqs.listener;

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

    @SqsListener("${app.sqs.purchase-queue}")
    public void onMessage(PurchaseMessage message) {
        log.info("Received purchase message: orderId={}, eventId={}",
                message.orderId(), message.eventId());

        processPurchaseUseCase.execute(message.orderId())
                .doOnSuccess(order -> log.info("Processed order: id={}, status={}",
                        order.id(), order.status()))
                .doOnError(e -> log.error("Error processing order: orderId={}, error={}",
                        message.orderId(), e.getMessage()))
                .block();
    }
}
