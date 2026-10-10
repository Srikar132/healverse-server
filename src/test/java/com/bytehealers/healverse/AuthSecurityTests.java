package com.bytehealers.healverse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> registerBody(String username, String password) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("gender", "MALE");
        profile.put("age", 30);
        profile.put("heightCm", 175);
        profile.put("currentWeightKg", 80);
        profile.put("targetWeightKg", 75);
        profile.put("activityLevel", "LIGHTLY_ACTIVE");
        profile.put("goal", "LOSE_WEIGHT");
        // The mobile client uses this key rather than "healthConditions"
        profile.put("healthCondition", "NONE");

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", username);
        user.put("password", password);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("profile", profile);
        return body;
    }

    private MvcResult register(Map<String, Object> body, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private MvcResult login(String username, String password, int expectedStatus) throws Exception {
        Map<String, Object> body = Map.of("username", username, "password", password);
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    @Test
    void registerIgnoresClientSuppliedIdAndCannotOverwriteAnotherUser() throws Exception {
        String victim = uniqueUsername("victim");
        long victimId = data(register(registerBody(victim, "victim-pass"), 200)).get("user").get("id").asLong();

        String attacker = uniqueUsername("attacker");
        Map<String, Object> body = registerBody(attacker, "attacker-pass");
        @SuppressWarnings("unchecked")
        Map<String, Object> userPart = (Map<String, Object>) body.get("user");
        userPart.put("id", victimId);
        long attackerId = data(register(body, 200)).get("user").get("id").asLong();

        assertNotEquals(victimId, attackerId);
        // Victim's credentials are untouched
        login(victim, "victim-pass", 200);
        login(victim, "attacker-pass", 401);
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        String username = uniqueUsername("dup");
        register(registerBody(username, "first-pass"), 200);
        register(registerBody(username, "second-pass"), 409);
    }

    @Test
    void registerValidatesCredentials() throws Exception {
        register(registerBody(uniqueUsername("short"), "123"), 400);
        register(registerBody("  ", "valid-pass"), 400);
    }

    @Test
    void registerStoresProfileFromClientPayload() throws Exception {
        JsonNode user = data(register(registerBody(uniqueUsername("prof"), "valid-pass"), 200)).get("user");
        assertEquals("LOSE_WEIGHT", user.get("profile").get("goal").asText());
        assertEquals("NONE", user.get("profile").get("healthCondition").asText());
    }

    @Test
    void voiceChatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/voice-chat/ask-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/voice-chat/default")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/voice-chat/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void voiceChatAcceptsValidToken() throws Exception {
        String token = data(register(registerBody(uniqueUsername("voice"), "valid-pass"), 200)).get("token").asText();

        mockMvc.perform(get("/api/voice-chat/health").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/voice-chat/bad%20session").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkAuthReturnsSameTokenInsteadOfReissuing() throws Exception {
        String token = data(register(registerBody(uniqueUsername("check"), "valid-pass"), 200)).get("token").asText();

        MvcResult result = mockMvc.perform(get("/auth/check-auth").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(token, data(result).get("token").asText());
    }

    @Test
    void invalidTokenDoesNotBlockPublicRoutesButIsRejectedOnProtectedOnes() throws Exception {
        String username = uniqueUsername("stale");
        register(registerBody(username, "valid-pass"), 200);

        // A stale token on a public route must not turn a valid login into a 401
        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "valid-pass"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/user/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsThrottledAfterRepeatedFailures() throws Exception {
        String username = uniqueUsername("brute");
        register(registerBody(username, "valid-pass"), 200);

        for (int i = 0; i < 5; i++) {
            login(username, "wrong-pass", 401);
        }
        // Locked out, even with the right password
        login(username, "valid-pass", 429);
    }
}
