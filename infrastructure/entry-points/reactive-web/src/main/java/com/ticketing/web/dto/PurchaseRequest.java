package com.ticketing.web.dto;

public record PurchaseRequest(
        String eventId,
        String userId,
        int quantity,
        String idempotencyKey
) {
}
