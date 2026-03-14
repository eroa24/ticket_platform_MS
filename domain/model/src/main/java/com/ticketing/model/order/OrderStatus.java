package com.ticketing.model.order;

public enum OrderStatus {

    PENDING,
    PROCESSING,
    CONFIRMED,
    REJECTED,
    EXPIRED;

    /**
     * Returns true if the order is in a terminal state and cannot be further modified.
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == REJECTED || this == EXPIRED;
    }
}
