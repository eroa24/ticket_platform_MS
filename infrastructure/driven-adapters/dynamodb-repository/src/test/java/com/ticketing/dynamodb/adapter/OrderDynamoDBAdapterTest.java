package com.ticketing.dynamodb.adapter;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.ticketing.dynamodb.entity.OrderEntity;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDynamoDBAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient enhancedClient;

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @Mock
    private DynamoDbAsyncTable<OrderEntity> table;

    @Mock
    private DynamoDbAsyncIndex<OrderEntity> idempotencyIndex;

    @Mock
    private DynamoDbAsyncIndex<OrderEntity> statusCreatedIndex;

    private OrderDynamoDBAdapter adapter;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("orders"), any(TableSchema.class))).thenReturn(table);
        when(table.index("idempotencyKey-index")).thenReturn(idempotencyIndex);
        when(table.index("status-createdAt-index")).thenReturn(statusCreatedIndex);
        adapter = new OrderDynamoDBAdapter(enhancedClient, dynamoDbClient);
    }

    @Test
    void save_ShouldSuccessfullySaveOrder() {
        PurchaseOrder order = new PurchaseOrder("1", "e1", "u1", 1, OrderStatus.PENDING, TicketStatus.RESERVED, "key", Instant.now(), Instant.now(), 1L);
        when(table.putItem(any(OrderEntity.class))).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.save(order))
                .expectNext(order)
                .verifyComplete();

        verify(table).putItem(any(OrderEntity.class));
    }

    @Test
    void findById_WhenExists_ShouldReturnOrder() {
        String id = "1";
        OrderEntity entity = createOrderEntity(id);

        when(table.getItem(any(software.amazon.awssdk.enhanced.dynamodb.Key.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));

        StepVerifier.create(adapter.findById(id))
                .expectNextMatches(o -> o.id().equals(id))
                .verifyComplete();
    }

    @Test
    void findByIdempotencyKey_WhenExists_ShouldReturnOrder() {
        String key = "key";
        OrderEntity entity = createOrderEntity("1");
        entity.setIdempotencyKey(key);

        Page<OrderEntity> page = Page.create(java.util.List.of(entity));
        PagePublisher<OrderEntity> publisher = mock(PagePublisher.class);
        doAnswer(invocation -> {
            org.reactivestreams.Subscriber<Page<OrderEntity>> subscriber = invocation.getArgument(0);
            Flux.just(page).subscribe(subscriber);
            return null;
        }).when(publisher).subscribe(any(org.reactivestreams.Subscriber.class));
        when(idempotencyIndex.query(any(QueryEnhancedRequest.class))).thenReturn(publisher);

        StepVerifier.create(adapter.findByIdempotencyKey(key))
                .expectNextMatches(o -> o.id().equals("1"))
                .verifyComplete();
    }

    @Test
    void findExpiredReservations_ShouldReturnFluxOfOrders() {
        OrderEntity entity = createOrderEntity("1");
        entity.setStatus(OrderStatus.PENDING.name());

        Page<OrderEntity> page = Page.create(java.util.List.of(entity));
        PagePublisher<OrderEntity> publisher = mock(PagePublisher.class);
        doAnswer(invocation -> {
            org.reactivestreams.Subscriber<Page<OrderEntity>> subscriber = invocation.getArgument(0);
            Flux.just(page).subscribe(subscriber);
            return null;
        }).when(publisher).subscribe(any(org.reactivestreams.Subscriber.class));
        when(statusCreatedIndex.query(any(QueryEnhancedRequest.class))).thenReturn(publisher);

        StepVerifier.create(adapter.findExpiredReservations(15L))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void updateStatus_WhenSuccessful_ShouldReturnUpdatedOrder() {
        PurchaseOrder order = new PurchaseOrder("1", "e1", "u1", 1, OrderStatus.CONFIRMED, TicketStatus.SOLD, "key", Instant.now(), Instant.now(), 1L);
        Map<String, AttributeValue> attrs = Map.of(
                "id", AttributeValue.builder().s("1").build(),
                "eventId", AttributeValue.builder().s("e1").build(),
                "userId", AttributeValue.builder().s("u1").build(),
                "quantity", AttributeValue.builder().n("1").build(),
                "status", AttributeValue.builder().s("CONFIRMED").build(),
                "ticketStatus", AttributeValue.builder().s("SOLD").build(),
                "idempotencyKey", AttributeValue.builder().s("key").build(),
                "createdAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                "updatedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                "version", AttributeValue.builder().n("2").build()
        );
        UpdateItemResponse response = UpdateItemResponse.builder()
                .attributes(attrs)
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.updateStatus(order, 1L))
                .expectNextMatches(o -> o.status() == OrderStatus.CONFIRMED && o.version() == 2L)
                .verifyComplete();
    }

    @Test
    void updateStatus_WhenConditionalCheckFails_ShouldReturnError() {
        PurchaseOrder order = new PurchaseOrder("1", "e1", "u1", 1, OrderStatus.CONFIRMED, TicketStatus.SOLD, "key", Instant.now(), Instant.now(), 1L);
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(ConditionalCheckFailedException.builder().message("Conflict").build()));

        StepVerifier.create(adapter.updateStatus(order, 1L))
                .expectError(BusinessException.class)
                .verify();
    }

    private OrderEntity createOrderEntity(String id) {
        OrderEntity entity = new OrderEntity();
        entity.setId(id);
        entity.setEventId("e1");
        entity.setUserId("u1");
        entity.setQuantity(1);
        entity.setStatus(OrderStatus.PENDING.name());
        entity.setTicketStatus(TicketStatus.RESERVED.name());
        entity.setIdempotencyKey("key");
        entity.setCreatedAt(Instant.now().toString());
        entity.setUpdatedAt(Instant.now().toString());
        entity.setVersion(1L);
        return entity;
    }
}
