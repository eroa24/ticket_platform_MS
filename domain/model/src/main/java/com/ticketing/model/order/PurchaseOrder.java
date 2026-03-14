package com.ticketing.model.order;

import java.time.Instant;

/**
 * Domain model representing a purchase order for event tickets.
 *
 * @param id             unique order identifier
 * @param eventId        the event being purchased
 * @param userId         the user making the purchase
 * @param quantity       number of tickets requested
 * @param status         current order processing status
 * @param idempotencyKey client-provided key to prevent duplicate processing
 * @param createdAt      timestamp when the order was created
 * @param updatedAt      timestamp of the last status change
 * @param version        optimistic locking version for conditional writes
 */
public record PurchaseOrder(
        String id,
        String eventId,
        String userId,
        int quantity,
        OrderStatus status,
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
                idempotencyKey, now, now, 0L);
    }

    /**
     * Returns a copy of this order with an updated status and timestamp.
     *
     * @param newStatus the new status to transition to
     * @return new PurchaseOrder instance with updated status
     */
    public PurchaseOrder withStatus(OrderStatus newStatus) {
        return new PurchaseOrder(id, eventId, userId, quantity, newStatus,
                idempotencyKey, createdAt, Instant.now(), version);
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
}
