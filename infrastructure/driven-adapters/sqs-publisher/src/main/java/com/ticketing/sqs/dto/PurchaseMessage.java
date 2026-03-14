package com.ticketing.sqs.dto;

public record PurchaseMessage(
        String orderId,
        String eventId,
        String userId,
        int quantity,
        String idempotencyKey
) {
}
