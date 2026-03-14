package com.ticketing.dynamodb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Configuration
public class DynamoDBTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTableInitializer.class);

    private static final String EVENTS_TABLE = "events";
    private static final String ORDERS_TABLE = "orders";
    private static final String IDEMPOTENCY_INDEX = "idempotencyKey-index";
    private static final String STATUS_CREATED_INDEX = "status-createdAt-index";

    @Bean
    public CommandLineRunner initDynamoDBTables(DynamoDbAsyncClient client) {
        return args -> {
            createEventsTable(client);
            createOrdersTable(client);
        };
    }

    private void createEventsTable(DynamoDbAsyncClient client) {
        var request = CreateTableRequest.builder()
                .tableName(EVENTS_TABLE)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        client.createTable(request)
                .thenAccept(r -> log.info("Created table: {}", EVENTS_TABLE))
                .exceptionally(e -> {
                    if (e.getMessage().contains("Table already exists")) {
                        log.info("Table already exists: {}", EVENTS_TABLE);
                    } else {
                        log.error("Error creating table {}: {}", EVENTS_TABLE, e.getMessage());
                    }
                    return null;
                });
    }

    private void createOrdersTable(DynamoDbAsyncClient client) {
        var request = CreateTableRequest.builder()
                .tableName(ORDERS_TABLE)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("idempotencyKey")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("status")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("createdAt")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName(IDEMPOTENCY_INDEX)
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("idempotencyKey")
                                        .keyType(KeyType.HASH)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .build(),
                        GlobalSecondaryIndex.builder()
                                .indexName(STATUS_CREATED_INDEX)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("status")
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName("createdAt")
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        client.createTable(request)
                .thenAccept(r -> log.info("Created table: {}", ORDERS_TABLE))
                .exceptionally(e -> {
                    if (e.getMessage().contains("Table already exists")) {
                        log.info("Table already exists: {}", ORDERS_TABLE);
                    } else {
                        log.error("Error creating table {}: {}", ORDERS_TABLE, e.getMessage());
                    }
                    return null;
                });
    }
}
