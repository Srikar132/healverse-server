package com.bytehealers.healverse;

import com.bytehealers.healverse.service.GamificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Behaviour of the auth flow around its collaborators: the points feature must never block sign-in,
 * browser preflight must not be rejected, and profile updates must not wipe omitted optional fields.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthResilienceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GamificationService gamificationService;

    private static String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, Object> profile() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("gender", "FEMALE");
        profile.put("age", 28);
        profile.put("heightCm", 165);
        profile.put("currentWeightKg", 62);
        profile.put("targetWeightKg", 58);
        profile.put("activityLevel", "MODERATELY_ACTIVE");
        profile.put("goal", "LOSE_WEIGHT");
        return profile;
    }

    private MvcResult register(String username, Map<String, Object> profile) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", Map.of("username", username, "password", "valid-pass"));
        body.put("profile", profile);
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void registerAndLoginSucceedWhenDailyLoginPointsFail() throws Exception {
        doThrow(new RuntimeException("points table unavailable"))
                .when(gamificationService).recordDailyLogin(anyLong());

        String username = uniqueUsername("points");
        register(username, profile());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "valid-pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void corsPreflightIsNotRejectedByTheSecurityChain() throws Exception {
        mockMvc.perform(options("/api/user/profile")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void profileUpdateKeepsOptionalFieldsTheClientOmits() throws Exception {
        Map<String, Object> full = profile();
        full.put("healthCondition", "DIABETES");
        full.put("address", "12 Lake Road");

        String token = objectMapper.readTree(register(uniqueUsername("keep"), full).getResponse().getContentAsString())
                .get("data").get("token").asText();

        // Same payload without healthCondition / address
        MvcResult updated = mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(updated.getResponse().getContentAsString()).get("data");
        assertEquals("DIABETES", data.get("healthCondition").asText());
        assertEquals("12 Lake Road", data.get("address").asText());
    }
}
