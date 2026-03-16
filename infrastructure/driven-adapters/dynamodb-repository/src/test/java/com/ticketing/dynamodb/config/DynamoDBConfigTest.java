package com.ticketing.dynamodb.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DynamoDBConfigTest {

    @Test
    void testBeansCreation() {
        DynamoDBConfig config = new DynamoDBConfig();
        ReflectionTestUtils.setField(config, "endpoint", "http://localhost:8000");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKey", "key");
        ReflectionTestUtils.setField(config, "secretKey", "secret");

        DynamoDbAsyncClient client = config.dynamoDbAsyncClient();
        assertNotNull(client);
        assertNotNull(config.dynamoDbEnhancedAsyncClient(client));
    }
}
