package com.ticketing.web.filter;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.ticketing.model.secrets.AppSecrets;
import com.ticketing.web.dto.ErrorResponse;

import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimiterFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    private static final long WINDOW_MILLIS = 60_000L;
    private static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_RATE_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String ACTUATOR_PREFIX = "/actuator";
    private static final String ERROR_CODE_RATE_LIMIT = "ERR_RATE_LIMIT";
    private static final String ERROR_MSG_RATE_LIMIT = "Too many requests. Please try again later.";
    private static final String RETRY_AFTER_SECONDS = "30";

    private final ConcurrentHashMap<String, long[]> clientWindows = new ConcurrentHashMap<>();

    private final int maxRequestsPerWindow;
    private final JsonMapper jsonMapper;

    public RateLimiterFilter(JsonMapper jsonMapper, AppSecrets appSecrets) {
        this.jsonMapper = jsonMapper;
        this.maxRequestsPerWindow = appSecrets.rateLimitRequestsPerMinute();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isActuatorRequest(exchange)) {
            return chain.filter(exchange);
        }

        var clientIp = extractClientIp(exchange);
        var currentCount = incrementAndGet(clientIp);

        addRateLimitHeaders(exchange, currentCount);

        return currentCount > maxRequestsPerWindow
                ? rejectWithTooManyRequests(exchange, clientIp)
                : chain.filter(exchange);
    }

    /**
     * @return the updated request count for the current window
     */
    private long incrementAndGet(String clientIp) {
        var now = System.currentTimeMillis();
        var window = clientWindows.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[0] >= WINDOW_MILLIS) {
                return new long[]{now, 1L};
            }
            return new long[]{existing[0], existing[1] + 1};
        });
        return window[1];
    }

    private void addRateLimitHeaders(ServerWebExchange exchange, long currentCount) {
        var headers = exchange.getResponse().getHeaders();
        headers.set(HEADER_RATE_LIMIT, String.valueOf(maxRequestsPerWindow));
        headers.set(HEADER_RATE_REMAINING, String.valueOf(Math.max(0, maxRequestsPerWindow - currentCount)));
    }

    private String extractClientIp(ServerWebExchange exchange) {
        var forwardedFor = exchange.getRequest().getHeaders().getFirst(HEADER_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private boolean isActuatorRequest(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith(ACTUATOR_PREFIX);
    }

    private Mono<Void> rejectWithTooManyRequests(ServerWebExchange exchange, String clientIp) {
        log.warn("Rate limit exceeded: clientIp={}, limit={}/min",
                clientIp, maxRequestsPerWindow);
        return Mono.defer(() -> {
            try {
                var response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                response.getHeaders().set(HEADER_RETRY_AFTER, RETRY_AFTER_SECONDS);
                var error = ErrorResponse.of(ERROR_CODE_RATE_LIMIT, ERROR_MSG_RATE_LIMIT);
                var json = jsonMapper.writeValueAsBytes(error);
                var buffer = response.bufferFactory().wrap(json);
                return response.writeWith(Mono.just(buffer));
            } catch (Exception e) {
                log.error("Failed to serialize rate limit response", e);
                return Mono.error(e);
            }
        });
    }
}
