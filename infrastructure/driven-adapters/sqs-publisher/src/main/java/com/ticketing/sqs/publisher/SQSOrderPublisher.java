package com.ticketing.sqs.publisher;

import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderMessageGateway;
import com.ticketing.sqs.dto.PurchaseMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ticketing.model.secrets.AppSecrets;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import reactor.core.publisher.Mono;

@Component
public class SQSOrderPublisher implements OrderMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(SQSOrderPublisher.class);

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public SQSOrderPublisher(SqsTemplate sqsTemplate, AppSecrets appSecrets) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = appSecrets.purchaseQueueName();
    }

    @Override
    public Mono<Void> sendPurchaseRequest(PurchaseOrder order) {
        return Mono.fromFuture(() -> sqsTemplate.sendAsync(queueName, toMessage(order)))
                .doOnSuccess(r -> log.info("Published order to SQS: orderId={}, queue={}",
                        order.id(), queueName))
                .doOnError(e -> log.error("Failed to publish order to SQS: orderId={}, error={}",
                        order.id(), e.getMessage()))
                .then();
    }

    private PurchaseMessage toMessage(PurchaseOrder order) {
        return new PurchaseMessage(
                order.id(),
                order.eventId(),
                order.userId(),
                order.quantity(),
                order.idempotencyKey());
    }
}
