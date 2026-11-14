package com.bytehealers.healverse.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiJsonTests {

    private static final String OBJECT = "{\"meals\": [{\"mealName\": \"Oats {bowl}\"}]}";

    @Test
    void plainJsonIsReturnedAsIs() {
        assertEquals(Optional.of(OBJECT), AiJson.extractObject(OBJECT));
    }

    @Test
    void fencedJsonWithAndWithoutClosingFence() {
        assertEquals(Optional.of(OBJECT), AiJson.extractObject("```json\n" + OBJECT + "\n```"));
        assertEquals(Optional.of(OBJECT), AiJson.extractObject("```json\n" + OBJECT));
        assertEquals(Optional.of(OBJECT), AiJson.extractObject("```\n" + OBJECT + "\n```\n"));
    }

    @Test
    void proseAroundTheObjectIsDropped() {
        assertEquals(Optional.of(OBJECT), AiJson.extractObject("Here is your plan:\n" + OBJECT + "\nEnjoy!"));
    }

    @Test
    void blankAndNonObjectInput() {
        assertTrue(AiJson.extractObject(null).isEmpty());
        assertTrue(AiJson.extractObject("  \n").isEmpty());
        assertEquals(Optional.of("I cannot help with that"), AiJson.extractObject(" I cannot help with that "));
    }
}
