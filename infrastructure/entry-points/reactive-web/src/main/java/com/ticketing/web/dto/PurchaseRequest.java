package com.ticketing.web.dto;

import com.ticketing.model.common.DomainConstants;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
        @NotBlank(message = "eventId is required")
        String eventId,

        @NotBlank(message = "userId is required")
        String userId,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = DomainConstants.MAX_TICKETS_PER_PURCHASE, message = "quantity must not exceed {max}")
        int quantity,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) {
}
