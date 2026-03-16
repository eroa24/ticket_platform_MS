package com.ticketing.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CorrelationFilterTest {

    private CorrelationFilter filter;

    @Mock
    private WebFilterChain chain;

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @BeforeEach
    void setUp() {
        filter = new CorrelationFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_generatesNewIdWhenHeaderIsMissing() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/events").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER_CORRELATION_ID)).isNotNull();
        verify(chain).filter(exchange);
    }

    @Test
    void filter_usesExistingIdWhenHeaderIsPresent() {
        var existingId = "existing-uuid";
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/events")
                        .header(HEADER_CORRELATION_ID, existingId)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER_CORRELATION_ID)).isEqualTo(existingId);
    }

    @Test
    void filter_skipsCorrelationForActuator() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER_CORRELATION_ID)).isNull();
    }
}
