package com.ticketing.model.ticket;

import java.util.Map;
import java.util.Set;

public enum TicketStatus {

    AVAILABLE,
    RESERVED,
    PENDING_CONFIRMATION,
    SOLD,
    COMPLIMENTARY;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            AVAILABLE, Set.of(RESERVED, COMPLIMENTARY),
            RESERVED, Set.of(PENDING_CONFIRMATION, AVAILABLE),
            PENDING_CONFIRMATION, Set.of(SOLD, AVAILABLE)
    );

    /**
     * @param target the desired target state
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(TicketStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * Returns true if the current state is a terminal (final) state.
     */
    public boolean isFinal() {
        return this == SOLD || this == COMPLIMENTARY;
    }
}
