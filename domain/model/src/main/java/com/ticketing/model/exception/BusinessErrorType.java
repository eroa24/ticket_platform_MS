package com.ticketing.model.exception;

public enum BusinessErrorType {

    EVENT_NOT_FOUND("ERR_001", "Event not found with id: %s"),
    ORDER_NOT_FOUND("ERR_002", "Order not found with id: %s"),
    INSUFFICIENT_TICKETS("ERR_003", "Not enough tickets available for event: %s. Requested: %s, Available: %s"),
    INVALID_STATE_TRANSITION("ERR_004", "Invalid state transition from %s to %s"),
    DUPLICATE_ORDER("ERR_005", "Duplicate order detected for idempotency key: %s"),
    RESERVATION_EXPIRED("ERR_006", "Reservation has expired for order: %s"),
    MAX_TICKETS_EXCEEDED("ERR_007", "Maximum ticket quantity per purchase exceeded. Max allowed: %s"),
    INVALID_REQUEST("ERR_008", "Invalid request: %s"),
    OPTIMISTIC_LOCK_FAILURE("ERR_009", "Concurrent modification detected, please retry"),
    EVENT_ALREADY_EXISTS("ERR_010", "An event with id %s already exists");

    private final String code;
    private final String messageTemplate;

    BusinessErrorType(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String code() {
        return code;
    }

    public String messageTemplate() {
        return messageTemplate;
    }

    /**
     * @param args values to substitute into the message template
     * @return a new BusinessException instance
     */
    public BusinessException build(Object... args) {
        var formattedMessage = args.length > 0
                ? String.format(messageTemplate, args)
                : messageTemplate;
        return new BusinessException(this, formattedMessage);
    }
}
