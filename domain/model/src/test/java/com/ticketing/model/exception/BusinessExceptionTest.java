package com.ticketing.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    void businessException_storesPropertiesCorrectly() {
        var errorType = BusinessErrorType.EVENT_NOT_FOUND;
        var message = "Event not found";
        var exception = new BusinessException(errorType, message);

        assertThat(exception.errorType()).isEqualTo(errorType);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.code()).isEqualTo(errorType.code());
    }

    @Test
    void businessErrorType_buildsExceptionWithFormattedMessage() {
        var exception = BusinessErrorType.INSUFFICIENT_TICKETS.build("event-1", 5, 2);

        assertThat(exception.errorType()).isEqualTo(BusinessErrorType.INSUFFICIENT_TICKETS);
        assertThat(exception.getMessage()).contains("event-1").contains("5").contains("2");
        assertThat(exception.code()).isEqualTo("ERR_003");
    }

    @Test
    void businessErrorType_buildsExceptionWithoutArgs() {
        var exception = BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.build();

        assertThat(exception.errorType()).isEqualTo(BusinessErrorType.OPTIMISTIC_LOCK_FAILURE);
        assertThat(exception.getMessage()).isEqualTo(BusinessErrorType.OPTIMISTIC_LOCK_FAILURE.messageTemplate());
    }
}
