package com.bytehealers.healverse;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every failure must reach the client through GlobalExceptionHandler with the right status and the
 * ApiResponse envelope ({success:false, message, data:null}), without leaking internals.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DietPlanRepository dietPlanRepository;

    @Autowired
    private UserRepository userRepository;

    /** Registers a fresh user and returns {token, id}. */
    private Map<String, Object> registerUser() throws Exception {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("gender", "MALE");
        profile.put("age", 30);
        profile.put("heightCm", 175);
        profile.put("currentWeightKg", 80);
        profile.put("targetWeightKg", 75);
        profile.put("activityLevel", "LIGHTLY_ACTIVE");
        profile.put("goal", "LOSE_WEIGHT");
        profile.put("healthCondition", "NONE");

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", "err" + UUID.randomUUID().toString().substring(0, 8));
        user.put("password", "Passw0rd!x");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("profile", profile);

        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");
        return Map.of("token", data.get("token").asText(), "id", data.get("user").get("id").asLong());
    }

    private static String bearer(Map<String, Object> user) {
        return "Bearer " + user.get("token");
    }

    private static void assertErrorEnvelope(ResultActions result, int status, String message) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unknownRecordIs404NotBadRequest() throws Exception {
        Map<String, Object> user = registerUser();

        assertErrorEnvelope(
                mockMvc.perform(get("/api/food-logs/987654321").header("Authorization", bearer(user))),
                404, "Food log not found");
    }

    @Test
    void invalidPathValueIs400() throws Exception {
        Map<String, Object> user = registerUser();

        assertErrorEnvelope(
                mockMvc.perform(get("/api/water-logs/not-a-date").header("Authorization", bearer(user))),
                400, "Invalid value for 'date'");
    }

    @Test
    void missingRequiredParameterIs400() throws Exception {
        Map<String, Object> user = registerUser();

        assertErrorEnvelope(
                mockMvc.perform(post("/api/diet-plans/meals/generate").header("Authorization", bearer(user))),
                400, "Missing required parameter 'mealType'");
    }

    @Test
    void malformedJsonIs400WithoutParserDetails() throws Exception {
        assertErrorEnvelope(
                mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json")),
                400, "Malformed request body");
    }

    @Test
    void voiceAssistantIsSwitchedOffByDefault() throws Exception {
        Map<String, Object> user = registerUser();

        assertErrorEnvelope(
                mockMvc.perform(delete("/api/voice-chat/session1").header("Authorization", bearer(user))),
                410, "The voice assistant is currently unavailable");
        assertErrorEnvelope(
                mockMvc.perform(post("/api/voice-chat/text-to-speech")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}")),
                410, "The voice assistant is currently unavailable");
    }

    @Test
    void unsupportedImageTypeIs400BeforeAnyAiCall() throws Exception {
        Map<String, Object> user = registerUser();

        assertErrorEnvelope(
                mockMvc.perform(multipart("/api/food-logs/log/image")
                        .file(new MockMultipartFile("image", "meal.gif", "image/gif", new byte[]{1, 2, 3}))
                        .file(new MockMultipartFile("mealType", "", "text/plain", "LUNCH".getBytes()))
                        .header("Authorization", bearer(user))),
                400, "Unsupported image type. Use JPEG, PNG or WebP.");
    }

    @Test
    void cannotRegenerateAnotherUsersDietPlan() throws Exception {
        Map<String, Object> owner = registerUser();
        Map<String, Object> intruder = registerUser();

        DietPlan plan = new DietPlan();
        plan.setUser(userRepository.findById(((Number) owner.get("id")).longValue()).orElseThrow());
        plan.setPlanDate(LocalDate.now());
        plan.setTotalCalories(new BigDecimal("2000"));
        plan.setTotalProtein(new BigDecimal("100"));
        plan.setTotalCarbs(new BigDecimal("250"));
        plan.setTotalFat(new BigDecimal("60"));
        Long planId = dietPlanRepository.save(plan).getId();

        assertErrorEnvelope(
                mockMvc.perform(put("/api/diet-plans/" + planId + "/regenerate")
                        .header("Authorization", bearer(intruder))),
                404, "Diet plan not found");
        assertErrorEnvelope(
                mockMvc.perform(put("/api/diet-plans/" + planId + "/meals/BREAKFAST/regenerate")
                        .header("Authorization", bearer(intruder))),
                404, "Diet plan not found");
        assertEquals(1, dietPlanRepository.findAll().stream().filter(p -> p.getId().equals(planId)).count());
    }
}
