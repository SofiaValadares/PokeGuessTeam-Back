package com.svc.pokeguessteam.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção de regra de negócio. A mensagem exibida ao cliente vem de {@code messages.properties}
 * pela chave {@link #getMessageKey()}.
 */
public class ApiBusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String messageKey;
    private final Object[] messageArgs;
    private final Integer retryAfterSeconds;

    public ApiBusinessException(HttpStatus status, String code, String messageKey) {
        this(status, code, messageKey, new Object[0], null);
    }

    public ApiBusinessException(HttpStatus status, String code, String messageKey, Object... messageArgs) {
        this(status, code, messageKey, messageArgs, null);
    }

    public static ApiBusinessException withRetryAfter(
            HttpStatus status,
            String code,
            String messageKey,
            int retryAfterSeconds,
            Object... messageArgs
    ) {
        return new ApiBusinessException(status, code, messageKey, messageArgs, retryAfterSeconds);
    }

    private ApiBusinessException(
            HttpStatus status,
            String code,
            String messageKey,
            Object[] messageArgs,
            Integer retryAfterSeconds
    ) {
        super(messageKey);
        this.status = status;
        this.code = code;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
