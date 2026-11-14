package com.bytehealers.healverse.service;

import java.util.Optional;

/**
 * Pulls the JSON object out of an LLM reply. Models wrap JSON in markdown fences, sometimes omit the closing
 * fence, and sometimes add a sentence before or after it, so instead of stripping fences this takes everything
 * from the first '{' to the last '}'.
 */
final class AiJson {

    private AiJson() {
    }

    static Optional<String> extractObject(String response) {
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end < start) {
            return Optional.of(response.trim());   // not an object: let the JSON parser report it
        }
        return Optional.of(response.substring(start, end + 1));
    }
}
