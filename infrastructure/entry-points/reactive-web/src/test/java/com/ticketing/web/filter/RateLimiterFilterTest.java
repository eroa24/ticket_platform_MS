package com.ticketing.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    private RateLimiterFilter filter;
    private JsonMapper jsonMapper;

    @Mock
    private WebFilterChain chain;

    private static final int MAX_REQUESTS = 2;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        filter = new RateLimiterFilter(jsonMapper, MAX_REQUESTS);
    }

    @Test
    void filter_allowsRequestsWithinLimit() {
        var exchange1 = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build());
        var exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build());

        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange1, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, chain)).verifyComplete();

        assertThat(exchange1.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("1");
        assertThat(exchange2.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        verify(chain, times(2)).filter(any());
    }

    @Test
    void filter_rejectsRequestsExceedingLimit() {
        var exchange1 = MockServerWebExchange.from(MockServerHttpRequest.get("/api").remoteAddress(null).build());
        var exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/api").remoteAddress(null).build());
        var exchange3 = MockServerWebExchange.from(MockServerHttpRequest.get("/api").remoteAddress(null).build());

        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange1, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange3, chain)).verifyComplete();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange3.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
        verify(chain, times(2)).filter(any());
    }

    @Test
    void filter_skipsRateLimitingForActuator() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/info").build());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isNull();
        verify(chain).filter(exchange);
    }
}
