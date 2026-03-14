package com.ticketing.model.secrets;

/**
 * Domain model representing the application's centralized secrets.
 *
 * Loaded at startup from AWS Secrets Manager.
 * Replaces direct environment-variable injection for operational config
 * that changes per environment and must not live in source code.
 */
public record AppSecrets(
        String purchaseQueueName,
        int rateLimitRequestsPerMinute) {
}
