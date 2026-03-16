package com.ticketing.dynamodb.adapter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.ticketing.dynamodb.entity.EventEntity;
import com.ticketing.model.event.Event;
import com.ticketing.model.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDynamoDBAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient enhancedClient;

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @Mock
    private DynamoDbAsyncTable<EventEntity> table;

    private EventDynamoDBAdapter adapter;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("events"), any(TableSchema.class))).thenReturn(table);
        adapter = new EventDynamoDBAdapter(enhancedClient, dynamoDbClient);
    }

    @Test
    void save_ShouldSuccessfullySaveEvent() {
        Event event = new Event("1", "Concert", Instant.now(), "Venue", 100, 100, 1L);
        when(table.putItem(any(EventEntity.class))).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.save(event))
                .expectNext(event)
                .verifyComplete();

        verify(table).putItem(any(EventEntity.class));
    }

    @Test
    void findById_WhenExists_ShouldReturnEvent() {
        String id = "1";
        EventEntity entity = new EventEntity();
        entity.setId(id);
        entity.setName("Concert");
        entity.setDate(Instant.now().toString());
        entity.setVenue("Venue");
        entity.setTotalCapacity(100);
        entity.setAvailableTickets(100);
        entity.setVersion(1L);

        when(table.getItem(any(software.amazon.awssdk.enhanced.dynamodb.Key.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));

        StepVerifier.create(adapter.findById(id))
                .expectNextMatches(e -> e.id().equals(id))
                .verifyComplete();
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        when(table.getItem(any(software.amazon.awssdk.enhanced.dynamodb.Key.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.findById("1"))
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnFluxOfEvents() {
        EventEntity entity = new EventEntity();
        entity.setId("1");
        entity.setName("Concert");
        entity.setDate(Instant.now().toString());
        entity.setVenue("Venue");
        entity.setTotalCapacity(100);
        entity.setAvailableTickets(100);
        entity.setVersion(1L);

        Page<EventEntity> page = Page.create(java.util.List.of(entity));
        PagePublisher<EventEntity> publisher = mock(PagePublisher.class);
        when(publisher.items()).thenReturn(software.amazon.awssdk.core.async.SdkPublisher.adapt(Flux.just(entity)));
        when(table.scan()).thenReturn(publisher);

        StepVerifier.create(adapter.findAll())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void updateAvailableTickets_WhenSuccessful_ShouldReturnUpdatedEvent() {
        String eventId = "1";
        Map<String, AttributeValue> attrs = Map.of(
                "name", AttributeValue.builder().s("Concert").build(),
                "date", AttributeValue.builder().s(Instant.now().toString()).build(),
                "venue", AttributeValue.builder().s("Venue").build(),
                "totalCapacity", AttributeValue.builder().n("100").build(),
                "availableTickets", AttributeValue.builder().n("90").build(),
                "version", AttributeValue.builder().n("2").build()
        );
        UpdateItemResponse response = UpdateItemResponse.builder()
                .attributes(attrs)
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.updateAvailableTickets(eventId, -10, 1L))
                .expectNextMatches(e -> e.availableTickets() == 90 && e.version() == 2L)
                .verifyComplete();
    }

    @Test
    void updateAvailableTickets_WhenConditionalCheckFails_ShouldReturnError() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(ConditionalCheckFailedException.builder().message("Conflict").build()));

        StepVerifier.create(adapter.updateAvailableTickets("1", -10, 1L))
                .expectError(BusinessException.class)
                .verify();
    }
}
