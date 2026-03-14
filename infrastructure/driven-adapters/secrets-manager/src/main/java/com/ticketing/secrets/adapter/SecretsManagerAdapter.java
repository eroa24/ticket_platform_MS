package com.ticketing.secrets.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ticketing.model.secrets.AppSecrets;
import com.ticketing.model.secrets.gateway.SecretsGateway;
import com.ticketing.secrets.dto.AppSecretsDto;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import tools.jackson.databind.json.JsonMapper;

/**
 * Driven adapter that retrieves application secrets from AWS Secrets Manager.
 *
 * Reactive flow:
 *   GetSecretValue (async) → Mono.fromFuture → parse JSON → map to domain AppSecrets
 *
 * Uses the same Mono.fromFuture() bridge pattern as DynamoDB and SQS adapters,
 * keeping the entire bootstrap non-blocking until the single block() at startup.
 */
@Component
public class SecretsManagerAdapter implements SecretsGateway {

    private static final Logger log = LoggerFactory.getLogger(SecretsManagerAdapter.class);

    private final SecretsManagerAsyncClient client;
    private final JsonMapper jsonMapper;
    private final String secretName;

    public SecretsManagerAdapter(
            SecretsManagerAsyncClient client,
            JsonMapper jsonMapper,
            @Value("${aws.secretsmanager.secret-name:/ticket-platform/config}") String secretName) {
        this.client = client;
        this.jsonMapper = jsonMapper;
        this.secretName = secretName;
    }

    @Override
    public Mono<AppSecrets> loadSecrets() {
        var request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        return Mono.fromFuture(() -> client.getSecretValue(request))
                .map(response -> parseSecrets(response.secretString()))
                .doOnSuccess(s -> log.info("Secrets loaded: secretName={}, purchaseQueue={}, rateLimit={}/min",
                        secretName, s.purchaseQueueName(), s.rateLimitRequestsPerMinute()))
                .doOnError(e -> log.error("Failed to load secrets: secretName={}, error={}",
                        secretName, e.getMessage()));
    }

    private AppSecrets parseSecrets(String secretString) {
        try {
            var dto = jsonMapper.readValue(secretString, AppSecretsDto.class);
            return new AppSecrets(dto.purchaseQueueName(), dto.rateLimitRequestsPerMinute());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse secrets from Secrets Manager: secretName=" + secretName, e);
        }
    }
}
