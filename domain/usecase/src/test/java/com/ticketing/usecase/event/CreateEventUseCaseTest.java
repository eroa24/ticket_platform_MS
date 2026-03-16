package com.ticketing.usecase.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.ticketing.model.event.Event;
import com.ticketing.model.event.gateway.EventGateway;
import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateEventUseCaseTest {

    @Mock
    private EventGateway eventGateway;

    @InjectMocks
    private CreateEventUseCase useCase;

    private static final Instant FUTURE_DATE = Instant.parse("2027-06-15T20:00:00Z");

    @Test
    void execute_savesEventWithGeneratedId() {
        var input = Event.create(null, "Rock Concert", FUTURE_DATE, "Movistar Arena", 500);
        when(eventGateway.save(any(Event.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(input))
                .assertNext(saved -> {
                    assertThat(saved.id()).isNotNull().isNotBlank();
                    assertThat(saved.name()).isEqualTo("Rock Concert");
                    assertThat(saved.venue()).isEqualTo("Movistar Arena");
                    assertThat(saved.totalCapacity()).isEqualTo(500);
                    assertThat(saved.availableTickets()).isEqualTo(500);
                })
                .verifyComplete();

        verify(eventGateway).save(any(Event.class));
    }

    @Test
    void execute_failsWhenNameIsBlank() {
        var input = Event.create(null, "  ", FUTURE_DATE, "Movistar Arena", 500);

        StepVerifier.create(useCase.execute(input))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }

    @Test
    void execute_failsWhenVenueIsNull() {
        var input = Event.create(null, "Rock Concert", FUTURE_DATE, null, 500);

        StepVerifier.create(useCase.execute(input))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }

    @Test
    void execute_failsWhenCapacityIsZero() {
        var input = Event.create(null, "Rock Concert", FUTURE_DATE, "Movistar Arena", 0);

        StepVerifier.create(useCase.execute(input))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }

    @Test
    void execute_failsWhenDateIsNull() {
        var input = Event.create(null, "Rock Concert", null, "Movistar Arena", 500);

        StepVerifier.create(useCase.execute(input))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).errorType()).isEqualTo(BusinessErrorType.INVALID_REQUEST);
                })
                .verify();
    }
}
