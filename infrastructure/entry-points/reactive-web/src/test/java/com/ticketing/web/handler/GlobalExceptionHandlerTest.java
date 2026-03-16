package com.ticketing.web.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.model.exception.BusinessErrorType;
import com.ticketing.model.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        exceptionHandler = new GlobalExceptionHandler(jsonMapper);
    }

    @Test
    void handle_businessException_notFound() {
        var ex = BusinessErrorType.EVENT_NOT_FOUND.build("123");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString()).contains("application/json");
    }

    @Test
    void handle_businessException_conflict() {
        var ex = BusinessErrorType.INSUFFICIENT_TICKETS.build("event-1", 10, 5);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handle_businessException_okForDuplicate() {
        var ex = BusinessErrorType.DUPLICATE_ORDER.build("key-1");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void handle_genericException_internalError() {
        var ex = new RuntimeException("Unexpected");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
