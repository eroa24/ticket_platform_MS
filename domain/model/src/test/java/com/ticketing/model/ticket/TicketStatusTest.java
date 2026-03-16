package com.ticketing.model.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TicketStatusTest {

    @Test
    void canTransitionTo_availableTransitions() {
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.RESERVED)).isTrue();
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.COMPLIMENTARY)).isTrue();
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.SOLD)).isFalse();
    }

    @Test
    void canTransitionTo_reservedTransitions() {
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.PENDING_CONFIRMATION)).isTrue();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.SOLD)).isTrue();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.AVAILABLE)).isTrue();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.COMPLIMENTARY)).isFalse();
    }

    @Test
    void canTransitionTo_pendingConfirmationTransitions() {
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.SOLD)).isTrue();
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.AVAILABLE)).isTrue();
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.RESERVED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"SOLD", "COMPLIMENTARY"})
    void canTransitionTo_terminalStatesHaveNoTransitions(TicketStatus status) {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(status.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void isFinal_checksCorrectStates() {
        assertThat(TicketStatus.SOLD.isFinal()).isTrue();
        assertThat(TicketStatus.COMPLIMENTARY.isFinal()).isTrue();
        assertThat(TicketStatus.AVAILABLE.isFinal()).isFalse();
        assertThat(TicketStatus.RESERVED.isFinal()).isFalse();
        assertThat(TicketStatus.PENDING_CONFIRMATION.isFinal()).isFalse();
    }
}
