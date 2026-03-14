package com.ticketing.web.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import com.ticketing.model.exception.BusinessException;
import com.ticketing.web.dto.ErrorResponse;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        return switch (ex) {
            case BusinessException bex -> handleBusinessError(exchange, bex);
            default -> handleGenericError(exchange, ex);
        };
    }

    private Mono<Void> handleBusinessError(ServerWebExchange exchange, BusinessException ex) {
        var status = resolveStatus(ex);
        var error = ErrorResponse.of(ex.code(), ex.getMessage());
        log.warn("Business error: code={}, message={}", ex.code(), ex.getMessage());
        return writeResponse(exchange, status, error);
    }

    private Mono<Void> handleGenericError(ServerWebExchange exchange, Throwable ex) {
        var error = ErrorResponse.of("ERR_INTERNAL", "An unexpected error occurred");
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return writeResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    private HttpStatus resolveStatus(BusinessException ex) {
        return switch (ex.errorType()) {
            case EVENT_NOT_FOUND, ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INSUFFICIENT_TICKETS, OPTIMISTIC_LOCK_FAILURE -> HttpStatus.CONFLICT;
            case DUPLICATE_ORDER -> HttpStatus.OK;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, ErrorResponse error) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(toJson(error).getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String toJson(ErrorResponse error) {
        return "{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}"
                .formatted(error.code(), error.message(), error.timestamp());
    }
}
