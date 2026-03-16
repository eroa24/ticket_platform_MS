package com.ticketing.dynamodb.mapper;

import java.time.Instant;

import com.ticketing.dynamodb.entity.EventEntity;
import com.ticketing.dynamodb.entity.OrderEntity;
import com.ticketing.model.event.Event;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDBMapperTest {

    @Test
    void eventMapping_ShouldBeCorrect() {
        Event event = new Event("1", "Concert", Instant.now(), "Venue", 100, 100, 1L);
        
        EventEntity entity = DynamoDBMapper.toEntity(event);
        assertThat(entity.getId()).isEqualTo(event.id());
        assertThat(entity.getName()).isEqualTo(event.name());
        assertThat(entity.getDate()).isEqualTo(event.date().toString());
        assertThat(entity.getVenue()).isEqualTo(event.venue());
        assertThat(entity.getTotalCapacity()).isEqualTo(event.totalCapacity());
        assertThat(entity.getAvailableTickets()).isEqualTo(event.availableTickets());
        assertThat(entity.getVersion()).isEqualTo(event.version());

        Event domain = DynamoDBMapper.toDomain(entity);
        assertThat(domain).isEqualTo(event);
    }

    @Test
    void orderMapping_ShouldBeCorrect() {
        PurchaseOrder order = new PurchaseOrder("1", "e1", "u1", 2, OrderStatus.CONFIRMED, TicketStatus.SOLD, "key", Instant.now(), Instant.now(), 1L);

        OrderEntity entity = DynamoDBMapper.toEntity(order);
        assertThat(entity.getId()).isEqualTo(order.id());
        assertThat(entity.getEventId()).isEqualTo(order.eventId());
        assertThat(entity.getUserId()).isEqualTo(order.userId());
        assertThat(entity.getQuantity()).isEqualTo(order.quantity());
        assertThat(entity.getStatus()).isEqualTo(order.status().name());
        assertThat(entity.getTicketStatus()).isEqualTo(order.ticketStatus().name());
        assertThat(entity.getIdempotencyKey()).isEqualTo(order.idempotencyKey());
        assertThat(entity.getCreatedAt()).isEqualTo(order.createdAt().toString());
        assertThat(entity.getUpdatedAt()).isEqualTo(order.updatedAt().toString());
        assertThat(entity.getVersion()).isEqualTo(order.version());

        PurchaseOrder domain = DynamoDBMapper.toDomain(entity);
        assertThat(domain).isEqualTo(order);
    }

    @Test
    void toDomainOrder_WhenTicketStatusIsNull_ShouldFallbackToReserved() {
        OrderEntity entity = new OrderEntity();
        entity.setId("1");
        entity.setEventId("e1");
        entity.setUserId("u1");
        entity.setQuantity(2);
        entity.setStatus(OrderStatus.PENDING.name());
        entity.setTicketStatus(null);
        entity.setIdempotencyKey("key");
        entity.setCreatedAt(Instant.now().toString());
        entity.setUpdatedAt(Instant.now().toString());
        entity.setVersion(1L);

        PurchaseOrder domain = DynamoDBMapper.toDomain(entity);
        assertThat(domain.ticketStatus()).isEqualTo(TicketStatus.RESERVED);
    }
}
