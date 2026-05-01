package com.svc.pokeguessteam.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.svc.pokeguessteam.messages.MessageKeys;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApiBusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(ApiBusinessException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                ex.getMessageKey(),
                ex.getMessageArgs(),
                ex.getMessageKey(),
                locale
        );
        ApiErrorResponse body = ApiErrorResponse.of(ex.getCode(), message);
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> mapFieldError(fe, locale))
                .collect(Collectors.toList());

        String summary = fieldErrors.isEmpty()
                ? messageSource.getMessage(MessageKeys.VALIDATION_SUMMARY, null, locale)
                : fieldErrors.get(0).message();

        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCodes.VALIDATION_FAILED,
                summary,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(MessageKeys.VALIDATION_MALFORMED_JSON, null, locale);
        ApiErrorResponse body = ApiErrorResponse.of(ErrorCodes.MALFORMED_JSON, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ApiErrorResponse.FieldError mapFieldError(FieldError fe, Locale locale) {
        String field = fe.getField();
        String message = fe.getDefaultMessage();
        if (message == null || message.isBlank()) {
            message = messageSource.getMessage(MessageKeys.VALIDATION_FIELD_INVALID, null, locale);
        }
        return new ApiErrorResponse.FieldError(field, message);
    }
}
