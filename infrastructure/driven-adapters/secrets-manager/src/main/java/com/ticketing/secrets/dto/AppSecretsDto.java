package com.ticketing.secrets.dto;

import tools.jackson.annotation.JsonIgnoreProperties;

/**
 * Internal DTO for deserializing the JSON payload returned by AWS Secrets Manager.
 *
 * Kept in the infrastructure layer to isolate Jackson annotations from the domain model.
 * Field names match the JSON keys in the secret exactly so no @JsonProperty needed.
 *
 * Secret JSON structure:
 * {
 *   "purchaseQueueName": "purchase-orders-queue",
 *   "rateLimitRequestsPerMinute": 60
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppSecretsDto(
        String purchaseQueueName,
        int rateLimitRequestsPerMinute) {
}
