package com.bytehealers.healverse.exception;

/** The endpoint exists but its feature is switched off (maps to HTTP 410 Gone). */
public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String message) {
        super(message);
    }
}
