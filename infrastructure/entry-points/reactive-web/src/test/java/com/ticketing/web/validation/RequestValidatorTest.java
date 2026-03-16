package com.ticketing.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;
import com.ticketing.web.dto.CreateEventRequest;
import com.ticketing.web.dto.PurchaseRequest;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

import java.time.Instant;

class RequestValidatorTest {
    private static final String EVENT_ID = "event-1";
    private static final String USER_ID = "user-1";
    private static final String IDEMPOTENCY_KEY = "idem-key-1";
    private static RequestValidator validator;

    @BeforeAll
    static void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = new RequestValidator(factory.getValidator());
    }

    @Test
    void validate_purchaseRequest_passesForValidInput() {
        var request = new PurchaseRequest(EVENT_ID, USER_ID, 3, IDEMPOTENCY_KEY);

        StepVerifier.create(validator.validate(request))
                .assertNext(r -> assertThat(r.eventId()).isEqualTo(EVENT_ID))
                .verifyComplete();
    }

    @Test
    void validate_purchaseRequest_failsWhenEventIdIsBlank() {
        var request = new PurchaseRequest("  ", USER_ID, 3, IDEMPOTENCY_KEY);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                    assertThat(e.getMessage()).contains("eventId");
                })
                .verify();
    }

    @Test
    void validate_purchaseRequest_failsWhenUserIdIsBlank() {
        var request = new PurchaseRequest(EVENT_ID, "", 3, IDEMPOTENCY_KEY);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("userId");
                })
                .verify();
    }

    @Test
    void validate_purchaseRequest_failsWhenQuantityIsZero() {
        var request = new PurchaseRequest(EVENT_ID, USER_ID, 0, IDEMPOTENCY_KEY);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("quantity");
                })
                .verify();
    }

    @Test
    void validate_purchaseRequest_failsWhenQuantityExceedsMax() {
        var request = new PurchaseRequest(EVENT_ID, USER_ID, 11, IDEMPOTENCY_KEY);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("quantity");
                })
                .verify();
    }

    @Test
    void validate_purchaseRequest_failsWhenIdempotencyKeyIsNull() {
        var request = new PurchaseRequest(EVENT_ID, USER_ID, 3, null);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("idempotencyKey");
                })
                .verify();
    }

    @Test
    void validate_purchaseRequest_includesAllViolationsInMessage() {
        var request = new PurchaseRequest("", "", 0, "");

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    var message = e.getMessage();
                    assertThat(message).contains("eventId");
                    assertThat(message).contains("userId");
                    assertThat(message).contains("quantity");
                    assertThat(message).contains("idempotencyKey");
                })
                .verify();
    }

    // ── CreateEventRequest ───────────────────────────────────────────────────

    @Test
    void validate_createEventRequest_passesForValidInput() {
        var request = new CreateEventRequest("Rock Concert", Instant.now().plusSeconds(3600), "Arena", 500);

        StepVerifier.create(validator.validate(request))
                .assertNext(r -> assertThat(r.name()).isEqualTo("Rock Concert"))
                .verifyComplete();
    }

    @Test
    void validate_createEventRequest_failsWhenNameIsBlank() {
        var request = new CreateEventRequest("", Instant.now().plusSeconds(3600), "Arena", 500);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("name");
                })
                .verify();
    }

    @Test
    void validate_createEventRequest_failsWhenDateIsNull() {
        var request = new CreateEventRequest("Concert", null, "Arena", 500);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("date");
                })
                .verify();
    }

    @Test
    void validate_createEventRequest_failsWhenDateIsInThePast() {
        var request = new CreateEventRequest("Concert", Instant.now().minusSeconds(3600), "Arena", 500);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("date");
                })
                .verify();
    }

    @Test
    void validate_createEventRequest_failsWhenCapacityIsZero() {
        var request = new CreateEventRequest("Concert", Instant.now().plusSeconds(3600), "Arena", 0);

        StepVerifier.create(validator.validate(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(e.getMessage()).contains("totalCapacity");
                })
                .verify();
    }
}
