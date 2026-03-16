package com.ticketing.model.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class EventTest {

    private static final String ID = "event-1";
    private static final String NAME = "Rock Concert";
    private static final String VENUE = "Movistar Arena";
    private static final Instant DATE = Instant.parse("2027-06-15T20:00:00Z");
    private static final int CAPACITY = 100;

    @Test
    void create_setsAvailableTicketsEqualToTotalCapacity() {
        var event = Event.create(ID, NAME, DATE, VENUE, CAPACITY);

        assertThat(event.id()).isEqualTo(ID);
        assertThat(event.availableTickets()).isEqualTo(CAPACITY);
        assertThat(event.totalCapacity()).isEqualTo(CAPACITY);
        assertThat(event.version()).isZero();
    }

    @Test
    void hasAvailableTickets_returnsTrueWhenEnoughTickets() {
        var event = Event.create(ID, NAME, DATE, VENUE, CAPACITY);

        assertThat(event.hasAvailableTickets(10)).isTrue();
        assertThat(event.hasAvailableTickets(100)).isTrue();
    }

    @Test
    void hasAvailableTickets_returnsFalseWhenNotEnough() {
        var event = Event.create(ID, NAME, DATE, VENUE, CAPACITY);

        assertThat(event.hasAvailableTickets(101)).isFalse();
    }

    @Test
    void withReducedAvailability_decrementsByQuantity() {
        var event = Event.create(ID, NAME, DATE, VENUE, CAPACITY);

        var updated = event.withReducedAvailability(20);

        assertThat(updated.availableTickets()).isEqualTo(80);
        assertThat(updated.totalCapacity()).isEqualTo(CAPACITY);
        assertThat(updated.id()).isEqualTo(ID);
    }

    @Test
    void withIncreasedAvailability_incrementsByQuantity() {
        var event = new Event(ID, NAME, DATE, VENUE, CAPACITY, 80, 1L);

        var updated = event.withIncreasedAvailability(10);

        assertThat(updated.availableTickets()).isEqualTo(90);
    }
}
