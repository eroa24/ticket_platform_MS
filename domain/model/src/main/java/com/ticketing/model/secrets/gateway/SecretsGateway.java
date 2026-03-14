package com.ticketing.model.secrets.gateway;

import com.ticketing.model.secrets.AppSecrets;

import reactor.core.publisher.Mono;

/**
 * Port (output) for loading application secrets from an external secrets store.
 *
 * Decoupled from the underlying provider (AWS Secrets Manager, Vault, etc.)
 * following the same Ports & Adapters pattern used for DynamoDB and SQS.
 */
public interface SecretsGateway {

    Mono<AppSecrets> loadSecrets();
}
