package com.ticketing.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplimentaryRequest(
        @NotBlank(message = "eventId is required")
        String eventId,
        
        @NotBlank(message = "userId is required")
        String userId,
        
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,
        
        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) {
}
