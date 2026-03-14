package com.ticketing.web.validation;

import java.util.Set;
import java.util.stream.Collectors;

import com.ticketing.model.exception.BusinessException;
import org.springframework.stereotype.Component;

import com.ticketing.model.exception.BusinessErrorType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import reactor.core.publisher.Mono;

/**
 * Reactive wrapper around Jakarta Bean Validation.
 *
 * WebFlux functional routers do not trigger @Valid automatically (unlike @RestController).
 * This component invokes the Validator manually and converts constraint violations into
 * a BusinessException, so they flow through the existing GlobalExceptionHandler as HTTP 400.
 *
 * Usage in handlers:
 *   request.bodyToMono(PurchaseRequest.class)
 *       .flatMap(validator::validate)
 *       .flatMap(req -> useCase.execute(...))
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> Mono<T> validate(T request) {
        var violations = validator.validate(request);
        return violations.isEmpty()
                ? Mono.just(request)
                : Mono.defer(() -> Mono.error(toBusinessException(violations)));
    }

    private <T> BusinessException toBusinessException(
            Set<ConstraintViolation<T>> violations) {
        var message = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return BusinessErrorType.INVALID_REQUEST.build(message);
    }
}
