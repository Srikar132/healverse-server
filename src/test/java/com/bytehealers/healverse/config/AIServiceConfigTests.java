package com.bytehealers.healverse.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AIServiceConfigTests {

    private static AIServiceConfig withKey(String key) {
        AIServiceConfig config = new AIServiceConfig();
        ReflectionTestUtils.setField(config, "apiKey", key);
        return config;
    }

    @Test
    void startupFailsWhenKeyIsBlankOrAnUnresolvedPlaceholder() {
        assertThrows(IllegalStateException.class, () -> withKey("").requireApiKey());
        assertThrows(IllegalStateException.class, () -> withKey("   ").requireApiKey());
        assertThrows(IllegalStateException.class, () -> withKey("${GROQ_API_KEY}").requireApiKey());
    }

    @Test
    void startupSucceedsWithAKey() {
        assertDoesNotThrow(() -> withKey("gsk_test").requireApiKey());
    }
}
