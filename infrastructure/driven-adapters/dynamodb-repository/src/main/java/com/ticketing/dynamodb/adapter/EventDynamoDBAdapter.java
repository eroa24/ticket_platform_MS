package com.ticketing.dynamodb.adapter;

import java.util.Map;

import com.ticketing.dynamodb.entity.EventEntity;
import com.ticketing.dynamodb.mapper.DynamoDBMapper;
import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class EventDynamoDBAdapter implements EventGateway {

    private static final Logger log = LoggerFactory.getLogger(EventDynamoDBAdapter.class);
    private static final String TABLE_NAME = "events";

    private final DynamoDbAsyncTable<EventEntity> table;
    private final DynamoDbAsyncClient dynamoDbClient;

    public EventDynamoDBAdapter(DynamoDbEnhancedAsyncClient enhancedClient,
                                DynamoDbAsyncClient dynamoDbClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventEntity.class));
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<Event> save(Event event) {
        var entity = DynamoDBMapper.toEntity(event);
        return Mono.fromFuture(() -> table.putItem(entity))
                .thenReturn(event)
                .doOnSuccess(e -> log.info("Saved event: id={}", e.id()));
    }

    @Override
    public Mono<Event> findById(String id) {
        var key = software.amazon.awssdk.enhanced.dynamodb.Key.builder()
                .partitionValue(id)
                .build();
        return Mono.fromFuture(() -> table.getItem(key))
                .filter(java.util.Objects::nonNull)
                .map(DynamoDBMapper::toDomain);
    }

    @Override
    public Flux<Event> findAll() {
        return Flux.from(table.scan().items())
                .map(DynamoDBMapper::toDomain);
    }

    @Override
    public Mono<Event> updateAvailableTickets(String eventId, int ticketsDelta, long expectedVersion) {
        var updateRequest = buildConditionalUpdateRequest(eventId, ticketsDelta, expectedVersion);
        return Mono.fromFuture(() -> dynamoDbClient.updateItem(updateRequest))
                .map(response -> buildEventFromUpdateResponse(response.attributes(), eventId))
                .doOnSuccess(e -> log.info("Tickets updated: eventId={}, delta={}, availableTickets={}",
                        eventId, ticketsDelta, e.availableTickets()))
                .onErrorResume(ConditionalCheckFailedException.class, e ->
                        Mono.defer(() -> Mono.error(BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build())));
    }

    private UpdateItemRequest buildConditionalUpdateRequest(String eventId, int ticketsDelta,
                                                            long expectedVersion) {
        var isReservation = ticketsDelta < 0;
        var absDelta = Math.abs(ticketsDelta);

        var conditionExpression = isReservation
                ? "version = :expectedVersion AND availableTickets >= :absDelta"
                : "version = :expectedVersion";

        return UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s(eventId).build()))
                .updateExpression("SET availableTickets = availableTickets + :delta, version = version + :one")
                .conditionExpression(conditionExpression)
                .expressionAttributeValues(Map.of(
                        ":delta", AttributeValue.builder().n(String.valueOf(ticketsDelta)).build(),
                        ":one", AttributeValue.builder().n("1").build(),
                        ":expectedVersion", AttributeValue.builder().n(String.valueOf(expectedVersion)).build(),
                        ":absDelta", AttributeValue.builder().n(String.valueOf(absDelta)).build()))
                .returnValues(ReturnValue.ALL_NEW)
                .build();
    }

    private Event buildEventFromUpdateResponse(Map<String, AttributeValue> attrs, String eventId) {
        return new Event(
                eventId,
                attrs.get("name").s(),
                java.time.Instant.parse(attrs.get("date").s()),
                attrs.get("venue").s(),
                Integer.parseInt(attrs.get("totalCapacity").n()),
                Integer.parseInt(attrs.get("availableTickets").n()),
                Long.parseLong(attrs.get("version").n()));
    }
}
