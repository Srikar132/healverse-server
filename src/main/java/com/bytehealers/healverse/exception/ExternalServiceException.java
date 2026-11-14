package com.bytehealers.healverse.exception;

/**
 * A downstream dependency (the AI provider) failed or returned something unusable (maps to HTTP 502).
 * The message is shown to the client, so it must not contain provider details; the cause is only logged.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException(String message) {
        super(message);
    }
}
