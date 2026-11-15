package com.bytehealers.healverse.exception;

/** The request is well-formed but cannot be processed as given (maps to HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
