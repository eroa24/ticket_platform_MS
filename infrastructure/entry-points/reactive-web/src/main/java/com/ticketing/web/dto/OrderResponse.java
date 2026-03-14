package com.ticketing.web.dto;

import java.time.Instant;

import com.ticketing.model.order.OrderStatus;
import com.ticketing.model.order.PurchaseOrder;
import com.ticketing.model.ticket.TicketStatus;

/**
 * API response DTO for purchase order data.
 * Exposes both orderStatus (processing pipeline) and ticketStatus (ticket lifecycle)
 * so clients can track the full state of their purchase.
 *
 * @param orderId       unique order identifier
 * @param eventId       the purchased event
 * @param userId        the buyer
 * @param quantity      number of tickets
 * @param orderStatus   current processing status (PENDING → CONFIRMED / EXPIRED)
 * @param ticketStatus  current ticket state (RESERVED → SOLD / AVAILABLE)
 * @param createdAt     order creation timestamp
 * @param updatedAt     last status update timestamp
 */
public record OrderResponse(
        String orderId,
        String eventId,
        String userId,
        int quantity,
        OrderStatus orderStatus,
        TicketStatus ticketStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(
                order.id(),
                order.eventId(),
                order.userId(),
                order.quantity(),
                order.status(),
                order.ticketStatus(),
                order.createdAt(),
                order.updatedAt());
    }
}
