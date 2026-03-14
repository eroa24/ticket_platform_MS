package com.ticketing.dynamodb.mapper;

import java.time.Instant;

import com.ticketing.dynamodb.entity.EventEntity;
import com.ticketing.dynamodb.entity.OrderEntity;
import com.ticketing.model.event.Event;
import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;

public final class DynamoDBMapper {

    private DynamoDBMapper() {}

    public static EventEntity toEntity(Event event) {
        var entity = new EventEntity();
        entity.setId(event.id());
        entity.setName(event.name());
        entity.setDate(event.date().toString());
        entity.setVenue(event.venue());
        entity.setTotalCapacity(event.totalCapacity());
        entity.setAvailableTickets(event.availableTickets());
        entity.setVersion(event.version());
        return entity;
    }

    public static Event toDomain(EventEntity entity) {
        return new Event(
                entity.getId(),
                entity.getName(),
                Instant.parse(entity.getDate()),
                entity.getVenue(),
                entity.getTotalCapacity(),
                entity.getAvailableTickets(),
                entity.getVersion());
    }

    public static OrderEntity toEntity(PurchaseOrder order) {
        var entity = new OrderEntity();
        entity.setId(order.id());
        entity.setEventId(order.eventId());
        entity.setUserId(order.userId());
        entity.setQuantity(order.quantity());
        entity.setStatus(order.status().name());
        entity.setTicketStatus(order.ticketStatus().name());
        entity.setIdempotencyKey(order.idempotencyKey());
        entity.setCreatedAt(order.createdAt().toString());
        entity.setUpdatedAt(order.updatedAt().toString());
        entity.setVersion(order.version());
        return entity;
    }

    public static PurchaseOrder toDomain(OrderEntity entity) {
        return new PurchaseOrder(
                entity.getId(),
                entity.getEventId(),
                entity.getUserId(),
                entity.getQuantity(),
                OrderStatus.valueOf(entity.getStatus()),
                parseTicketStatus(entity.getTicketStatus()),
                entity.getIdempotencyKey(),
                Instant.parse(entity.getCreatedAt()),
                Instant.parse(entity.getUpdatedAt()),
                entity.getVersion());
    }

    /**
     * Parses ticketStatus with a safe fallback for records created before this field was introduced.
     */
    private static TicketStatus parseTicketStatus(String ticketStatus) {
        return ticketStatus != null ? TicketStatus.valueOf(ticketStatus) : TicketStatus.RESERVED;
    }
}
