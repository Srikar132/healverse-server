package com.bytehealers.healverse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With the voice flag on, the endpoints are live again and keep validating the session id. */
@SpringBootTest(properties = "app.voice.enabled=true")
@AutoConfigureMockMvc
class VoiceEnabledTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sessionsWorkAndInvalidIdIs400WhenEnabled() throws Exception {
        Map<String, Object> body = Map.of(
                "user", Map.of("username", "voice" + UUID.randomUUID().toString().substring(0, 8), "password", "Passw0rd!x"),
                "profile", Map.of("gender", "MALE", "age", 30, "heightCm", 175, "currentWeightKg", 80,
                        "targetWeightKg", 75, "activityLevel", "LIGHTLY_ACTIVE", "goal", "LOSE_WEIGHT",
                        "healthCondition", "NONE"));
        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(json).get("data").get("token");

        // Clearing the caller's own (empty) session succeeds
        mockMvc.perform(delete("/api/voice-chat/default").header("Authorization", "Bearer " + token.asText()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/voice-chat/bad$id").header("Authorization", "Bearer " + token.asText()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid session id"));
    }
}
