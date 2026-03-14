package com.ticketing.web.dto;

import java.time.Instant;

public record CreateEventRequest(
        String name,
        Instant date,
        String venue,
        int totalCapacity
) {
}
