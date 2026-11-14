package com.bytehealers.healverse.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityResponseHandlerTests {

    // Would break out of the string and add a field if it were concatenated into hand-built JSON
    private static final String HOSTILE = "bad\", \"admin\": true, \"x\": \"</script>";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void accessDeniedResponseIsValidJsonAndDoesNotEchoTheException() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/diet-plans");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException(HOSTILE));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("Access denied", body.get("message").asText());
        assertEquals("/api/diet-plans", body.get("path").asText());
        assertFalse(body.has("admin"));
        assertFalse(response.getContentAsString().contains("admin"));
    }

    @Test
    void unauthorizedResponseIsValidJsonAndDoesNotEchoTheException() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException(HOSTILE));

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("status").asInt());
        assertEquals("/api/user/profile", body.get("path").asText());
        assertFalse(body.has("details"));
        assertFalse(response.getContentAsString().contains("admin"));
    }
}
