package com.ticketing.model.common;

public final class DomainConstants {

    private DomainConstants() {}

    /** Maximum number of tickets allowed per single purchase. */
    public static final int MAX_TICKETS_PER_PURCHASE = 10;

    /** Default reservation timeout in minutes before automatic release. */
    public static final long RESERVATION_TIMEOUT_MINUTES = 10L;
}
