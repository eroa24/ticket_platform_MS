package com.ticketing.web.filter;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String ACTUATOR_PREFIX = "/actuator";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getPath().value().startsWith(ACTUATOR_PREFIX)) {
            return chain.filter(exchange);
        }

        var correlationId = Optional.ofNullable(
                        exchange.getRequest().getHeaders().getFirst(HEADER_CORRELATION_ID))
                .filter(v -> !v.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        exchange.getResponse().getHeaders().set(HEADER_CORRELATION_ID, correlationId);

        return chain.filter(exchange)
                .doOnSubscribe(s -> log.info("Incoming request: correlationId={}, method={}, path={}",
                        correlationId,
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath().value()))
                .doOnSuccess(__ -> log.info("Request completed: correlationId={}, status={}",
                        correlationId,
                        exchange.getResponse().getStatusCode()))
                .doOnError(e -> log.error("Request error: correlationId={}, error={}",
                        correlationId, e.getMessage()));
    }
}
