package com.ticketing.model.order;

import java.time.Instant;

import com.ticketing.model.ticket.TicketStatus;

/**
 * Domain model representing a purchase order for event tickets.
 *
 * @param id             unique order identifier
 * @param eventId        the event id
 * @param userId         the user id
 * @param quantity       number of tickets requested
 * @param status         current order status
 * @param ticketStatus   current ticket lifecycle state
 * @param idempotencyKey client-provided key to prevent duplicate processing
 * @param createdAt      timestamp created
 * @param updatedAt      timestamp last status change
 * @param version        optimistic locking version for conditional writes
 */
public record PurchaseOrder(
        String id,
        String eventId,
        String userId,
        int quantity,
        OrderStatus status,
        TicketStatus ticketStatus,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    /**
     * Factory method to create a new pending purchase order.
     */
    public static PurchaseOrder create(String id, String eventId, String userId,
                                       int quantity, String idempotencyKey) {
        var now = Instant.now();
        return new PurchaseOrder(id, eventId, userId, quantity, OrderStatus.PENDING,
                TicketStatus.RESERVED, idempotencyKey, now, now, 0L);
    }

    /**
     * @param newStatus the new order status to transition to
     * @return new PurchaseOrder instance with updated status and ticketStatus
     */
    public PurchaseOrder withStatus(OrderStatus newStatus) {
        return new PurchaseOrder(id, eventId, userId, quantity, newStatus,
                resolveTicketStatus(newStatus), idempotencyKey, createdAt, Instant.now(), version);
    }

    /**
     * Checks whether this reservation has expired based on the given timeout.
     *
     * @param reservationTimeoutMinutes maximum reservation duration in minutes
     * @return true if the reservation has exceeded the allowed time
     */
    public boolean isReservationExpired(long reservationTimeoutMinutes) {
        return status == OrderStatus.PENDING
                && createdAt.plusSeconds(reservationTimeoutMinutes * 60).isBefore(Instant.now());
    }

    /**
     * Maps an {@link OrderStatus} to the corresponding {@link TicketStatus} in the ticket lifecycle.
     */
    public static TicketStatus resolveTicketStatus(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING -> TicketStatus.RESERVED;
            case PROCESSING -> TicketStatus.PENDING_CONFIRMATION;
            case CONFIRMED -> TicketStatus.SOLD;
            case REJECTED, EXPIRED -> TicketStatus.AVAILABLE;
            case COMPLIMENTARY -> TicketStatus.COMPLIMENTARY;
        };
    }
}
