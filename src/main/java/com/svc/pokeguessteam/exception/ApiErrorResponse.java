package com.svc.pokeguessteam.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        String code,
        String message,
        List<FieldError> errors,
        Instant timestamp
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ApiErrorResponse of(String code, String message, List<FieldError> errors) {
        return new ApiErrorResponse(code, message, errors, Instant.now());
    }

    public record FieldError(String field, String message) {
    }
}
