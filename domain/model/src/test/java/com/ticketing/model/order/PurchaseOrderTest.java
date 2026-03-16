package com.ticketing.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.ticketing.model.ticket.TicketStatus;

import org.junit.jupiter.api.Test;

class PurchaseOrderTest {

    private static final String ID = "order-1";
    private static final String EVENT_ID = "event-1";
    private static final String USER_ID = "user-1";
    private static final String IDEMPOTENCY_KEY = "idem-key-1";
    private static final int QUANTITY = 2;
    private static final long TIMEOUT_MINUTES = 10L;

    @Test
    void create_initializesWithPendingStatusAndReservedTickets() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.ticketStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(order.version()).isZero();
        assertThat(order.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void withStatus_confirmed_setsTicketStatusToSold() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        var confirmed = order.withStatus(OrderStatus.CONFIRMED);

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.ticketStatus()).isEqualTo(TicketStatus.SOLD);
    }

    @Test
    void withStatus_expired_setsTicketStatusToAvailable() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        var expired = order.withStatus(OrderStatus.EXPIRED);

        assertThat(expired.status()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(expired.ticketStatus()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    void withStatus_rejected_setsTicketStatusToAvailable() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        var rejected = order.withStatus(OrderStatus.REJECTED);

        assertThat(rejected.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.ticketStatus()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    void withStatus_processing_setsTicketStatusToPendingConfirmation() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        var processing = order.withStatus(OrderStatus.PROCESSING);

        assertThat(processing.status()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(processing.ticketStatus()).isEqualTo(TicketStatus.PENDING_CONFIRMATION);
    }

    @Test
    void withStatus_complimentary_setsTicketStatusToComplimentary() {
        var order = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        var complimentary = order.withStatus(OrderStatus.COMPLIMENTARY);

        assertThat(complimentary.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
        assertThat(complimentary.ticketStatus()).isEqualTo(TicketStatus.COMPLIMENTARY);
    }

    @Test
    void isReservationExpired_returnsTrueWhenCreatedMoreThanTimeoutAgo() {
        var expiredOrder = new PurchaseOrder(
                ID, EVENT_ID, USER_ID, QUANTITY,
                OrderStatus.PENDING, TicketStatus.RESERVED,
                IDEMPOTENCY_KEY,
                Instant.now().minus(11, ChronoUnit.MINUTES),
                Instant.now(), 0L);

        assertThat(expiredOrder.isReservationExpired(TIMEOUT_MINUTES)).isTrue();
    }

    @Test
    void isReservationExpired_returnsFalseForRecentOrder() {
        var recentOrder = PurchaseOrder.create(ID, EVENT_ID, USER_ID, QUANTITY, IDEMPOTENCY_KEY);

        assertThat(recentOrder.isReservationExpired(TIMEOUT_MINUTES)).isFalse();
    }

    @Test
    void isReservationExpired_returnsFalseForTerminalStatus() {
        var confirmedOrder = new PurchaseOrder(
                ID, EVENT_ID, USER_ID, QUANTITY,
                OrderStatus.CONFIRMED, TicketStatus.SOLD,
                IDEMPOTENCY_KEY,
                Instant.now().minus(11, ChronoUnit.MINUTES),
                Instant.now(), 0L);

        assertThat(confirmedOrder.isReservationExpired(TIMEOUT_MINUTES)).isFalse();
    }

    @Test
    void orderStatus_isTerminal_forConfirmedRejectedExpired() {
        assertThat(OrderStatus.CONFIRMED.isTerminal()).isTrue();
        assertThat(OrderStatus.REJECTED.isTerminal()).isTrue();
        assertThat(OrderStatus.EXPIRED.isTerminal()).isTrue();
        assertThat(OrderStatus.PENDING.isTerminal()).isFalse();
        assertThat(OrderStatus.PROCESSING.isTerminal()).isFalse();
    }
}
