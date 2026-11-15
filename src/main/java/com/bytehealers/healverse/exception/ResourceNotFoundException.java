package com.bytehealers.healverse.exception;

/** The requested resource does not exist, or is not visible to the caller (maps to HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }
}
