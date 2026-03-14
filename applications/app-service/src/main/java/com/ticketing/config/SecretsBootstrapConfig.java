package com.ticketing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ticketing.model.secrets.AppSecrets;
import com.ticketing.model.secrets.gateway.SecretsGateway;

/**
 * Bootstraps application secrets at startup by calling AWS Secrets Manager
 * and exposing the result as a Spring bean available to all components.
 *
 * block() usage here is intentional and safe:
 *   - Runs during Spring context initialization, before the Netty event loop starts.
 *   - Fails fast: if secrets cannot be loaded, the application does not start.
 *   - One-time operation at startup — never called during request processing.
 *
 * Components that need secret values (SQSOrderPublisher, RateLimiterFilter)
 * inject AppSecrets directly, avoiding @Value and environment variable coupling.
 */
@Configuration
public class SecretsBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(SecretsBootstrapConfig.class);

    @Bean
    public AppSecrets appSecrets(SecretsGateway secretsGateway) {
        log.info("Loading application secrets from Secrets Manager...");
        return secretsGateway.loadSecrets()
                .doOnError(e -> log.error("Application startup aborted: secrets could not be loaded. error={}",
                        e.getMessage()))
                .block(); // Safe: Spring context init runs before Netty event loop
    }
}
