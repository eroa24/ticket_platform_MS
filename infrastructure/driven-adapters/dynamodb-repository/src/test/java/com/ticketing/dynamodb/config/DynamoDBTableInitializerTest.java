package com.ticketing.dynamodb.config;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDBTableInitializerTest {

    @Mock
    private DynamoDbAsyncClient client;

    @Test
    void testInitDynamoDBTables_WhenSuccess() throws Exception {
        DynamoDBTableInitializer initializer = new DynamoDBTableInitializer();
        
        when(client.createTable(any(CreateTableRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));

        CommandLineRunner runner = initializer.initDynamoDBTables(client);
        runner.run();

        verify(client, times(2)).createTable(any(CreateTableRequest.class));
    }

    @Test
    void testInitDynamoDBTables_WhenTableAlreadyExists() throws Exception {
        DynamoDBTableInitializer initializer = new DynamoDBTableInitializer();
        
        when(client.createTable(any(CreateTableRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Table already exists")));

        CommandLineRunner runner = initializer.initDynamoDBTables(client);
        runner.run();

        verify(client, times(2)).createTable(any(CreateTableRequest.class));
    }

    @Test
    void testInitDynamoDBTables_WhenOtherError() throws Exception {
        DynamoDBTableInitializer initializer = new DynamoDBTableInitializer();
        
        when(client.createTable(any(CreateTableRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Server error")));

        CommandLineRunner runner = initializer.initDynamoDBTables(client);
        runner.run();

        verify(client, times(2)).createTable(any(CreateTableRequest.class));
    }
}
