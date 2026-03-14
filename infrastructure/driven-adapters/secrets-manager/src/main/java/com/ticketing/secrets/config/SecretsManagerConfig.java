package com.ticketing.secrets.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;

/**
 * Configures the AWS Secrets Manager async client.
 *
 * Mirrors the same pattern used in DynamoDBConfig:
 *   - endpoint override for LocalStack in dev
 *   - static credentials (replaced by IAM roles in production)
 */
@Configuration
public class SecretsManagerConfig {

    @Value("${aws.secretsmanager.endpoint:http://localhost:4566}")
    private String endpoint;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.accessKey:local}")
    private String accessKey;

    @Value("${aws.secretKey:local}")
    private String secretKey;

    @Bean
    public SecretsManagerAsyncClient secretsManagerAsyncClient() {
        return SecretsManagerAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
