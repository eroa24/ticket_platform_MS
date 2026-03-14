package com.ticketing.model.exception;

public class BusinessException extends RuntimeException {

    private final BusinessErrorType errorType;

    public BusinessException(BusinessErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public BusinessErrorType errorType() {
        return errorType;
    }

    public String code() {
        return errorType.code();
    }
}
