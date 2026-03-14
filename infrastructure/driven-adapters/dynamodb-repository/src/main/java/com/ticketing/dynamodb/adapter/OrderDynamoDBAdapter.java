package com.ticketing.dynamodb.adapter;

import java.time.Instant;
import java.util.Map;

import com.ticketing.dynamodb.entity.OrderEntity;
import com.ticketing.dynamodb.mapper.DynamoDBMapper;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.order.gateway.OrderGateway;
import com.ticketing.model.ticket.TicketStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class OrderDynamoDBAdapter implements OrderGateway {

    private static final Logger log = LoggerFactory.getLogger(OrderDynamoDBAdapter.class);
    private static final String TABLE_NAME = "orders";
    private static final String IDEMPOTENCY_INDEX = "idempotencyKey-index";
    private static final String STATUS_CREATED_INDEX = "status-createdAt-index";

    private final DynamoDbAsyncTable<OrderEntity> table;
    private final DynamoDbAsyncIndex<OrderEntity> idempotencyIndex;
    private final DynamoDbAsyncIndex<OrderEntity> statusCreatedIndex;
    private final DynamoDbAsyncClient dynamoDbClient;

    public OrderDynamoDBAdapter(DynamoDbEnhancedAsyncClient enhancedClient,
                                DynamoDbAsyncClient dynamoDbClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(OrderEntity.class));
        this.idempotencyIndex = table.index(IDEMPOTENCY_INDEX);
        this.statusCreatedIndex = table.index(STATUS_CREATED_INDEX);
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<PurchaseOrder> save(PurchaseOrder order) {
        var entity = DynamoDBMapper.toEntity(order);
        return Mono.fromFuture(() -> table.putItem(entity))
                .thenReturn(order)
                .doOnSuccess(o -> log.info("Saved order: id={}, status={}", o.id(), o.status()));
    }

    @Override
    public Mono<PurchaseOrder> findById(String id) {
        var key = Key.builder().partitionValue(id).build();
        return Mono.fromFuture(() -> table.getItem(key))
                .filter(java.util.Objects::nonNull)
                .map(DynamoDBMapper::toDomain);
    }

    @Override
    public Mono<PurchaseOrder> findByIdempotencyKey(String idempotencyKey) {
        var queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(idempotencyKey).build());
        var request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(1)
                .build();
        return Flux.from(idempotencyIndex.query(request))
                .flatMap(page -> Flux.fromIterable(page.items()))
                .next()
                .map(DynamoDBMapper::toDomain);
    }

    @Override
    public Flux<PurchaseOrder> findExpiredReservations(long reservationTimeoutMinutes) {
        var cutoffTime = Instant.now().minusSeconds(reservationTimeoutMinutes * 60).toString();
        var queryConditional = QueryConditional.sortLessThanOrEqualTo(
                Key.builder()
                        .partitionValue(OrderStatus.PENDING.name())
                        .sortValue(cutoffTime)
                        .build());
        var request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();
        return Flux.from(statusCreatedIndex.query(request))
                .flatMap(page -> Flux.fromIterable(page.items()))
                .map(DynamoDBMapper::toDomain);
    }

    @Override
    public Mono<PurchaseOrder> updateStatus(PurchaseOrder order, long expectedVersion) {
        var updateRequest = buildConditionalStatusUpdate(order, expectedVersion);
        return Mono.fromFuture(() -> dynamoDbClient.updateItem(updateRequest))
                .map(response -> buildOrderFromUpdateResponse(response.attributes()))
                .doOnSuccess(o -> log.info("Order status updated: orderId={}, status={}, ticketStatus={}",
                        o.id(), o.status(), o.ticketStatus()))
                .onErrorResume(ConditionalCheckFailedException.class, e ->
                        Mono.defer(() -> Mono.error(BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build())));
    }

    private UpdateItemRequest buildConditionalStatusUpdate(PurchaseOrder order, long expectedVersion) {
        return UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s(order.id()).build()))
                .updateExpression("SET #s = :newStatus, ticketStatus = :ticketStatus, updatedAt = :updatedAt, version = version + :one")
                .conditionExpression("version = :expectedVersion")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":newStatus", AttributeValue.builder().s(order.status().name()).build(),
                        ":ticketStatus", AttributeValue.builder().s(order.ticketStatus().name()).build(),
                        ":updatedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                        ":one", AttributeValue.builder().n("1").build(),
                        ":expectedVersion", AttributeValue.builder().n(String.valueOf(expectedVersion)).build()))
                .returnValues(ReturnValue.ALL_NEW)
                .build();
    }

    private PurchaseOrder buildOrderFromUpdateResponse(Map<String, AttributeValue> attrs) {
        var ticketStatusAttr = attrs.get("ticketStatus");
        return new PurchaseOrder(
                attrs.get("id").s(),
                attrs.get("eventId").s(),
                attrs.get("userId").s(),
                Integer.parseInt(attrs.get("quantity").n()),
                OrderStatus.valueOf(attrs.get("status").s()),
                ticketStatusAttr != null
                        ? TicketStatus.valueOf(ticketStatusAttr.s())
                        : TicketStatus.RESERVED,
                attrs.get("idempotencyKey").s(),
                Instant.parse(attrs.get("createdAt").s()),
                Instant.parse(attrs.get("updatedAt").s()),
                Long.parseLong(attrs.get("version").n()));
    }
}
