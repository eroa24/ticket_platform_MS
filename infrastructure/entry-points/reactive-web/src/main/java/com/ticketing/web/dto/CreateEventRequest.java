package com.ticketing.web.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "date is required")
        @Future(message = "date must be in the future")
        Instant date,

        @NotBlank(message = "venue is required")
        String venue,

        @Min(value = 1, message = "totalCapacity must be at least 1")
        int totalCapacity
) {
}
