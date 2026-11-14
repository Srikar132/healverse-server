package com.bytehealers.healverse;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.MealRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The test configuration has spring.jpa.open-in-view=false, like production: lazy data must be loaded inside the
 * service layer, not while the response is serialized. These flows touch every lazy association that is returned.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenInViewOffFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DietPlanRepository dietPlanRepository;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private UserRepository userRepository;

    private record Account(String token, long id) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Account register() throws Exception {
        Map<String, Object> body = Map.of(
                "user", Map.of("username", "oiv" + UUID.randomUUID().toString().substring(0, 8), "password", "Passw0rd!x"),
                "profile", Map.of("gender", "MALE", "age", 30, "heightCm", 175, "currentWeightKg", 80,
                        "targetWeightKg", 75, "activityLevel", "LIGHTLY_ACTIVE", "goal", "LOSE_WEIGHT",
                        "healthCondition", "NONE"));
        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");
        return new Account(data.get("token").asText(), data.get("user").get("id").asLong());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, Account account, Object body) throws Exception {
        return mockMvc.perform(request.header("Authorization", account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    void waterLogsCanBeCreatedListedAndDeletedByTheirOwnerOnly() throws Exception {
        Account owner = register();
        Account other = register();

        String created = json(post("/api/water-logs/log"), owner, Map.of("amountMl", 250))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(get("/api/water-logs/today").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/water-logs/" + id).header("Authorization", other.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(delete("/api/water-logs/" + id).header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/water-logs/" + id).header("Authorization", owner.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exerciseLogsCanBeCreatedUpdatedAndDeletedByTheirOwnerOnly() throws Exception {
        Account owner = register();
        Account other = register();
        Map<String, Object> exercise = Map.of("exerciseName", "Running", "durationMinutes", 30, "intensity", "MODERATE");

        String created = json(post("/api/exercise-logs/log"), owner, exercise)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        json(put("/api/exercise-logs/" + id), owner,
                Map.of("exerciseName", "Cycling", "durationMinutes", 45, "intensity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseName").value("Cycling"));
        json(put("/api/exercise-logs/" + id), other, exercise).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/exercise-logs/" + id).header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/exercise-logs/" + id).header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void dietPlansSerializeTheirMealsWithoutAnOpenSession() throws Exception {
        Account owner = register();

        DietPlan plan = new DietPlan();
        plan.setUser(userRepository.findById(owner.id()).orElseThrow());
        plan.setPlanDate(LocalDate.now());
        plan.setTotalCalories(new BigDecimal("500"));
        plan.setTotalProtein(new BigDecimal("30"));
        plan.setTotalCarbs(new BigDecimal("60"));
        plan.setTotalFat(new BigDecimal("15"));
        plan = dietPlanRepository.save(plan);

        Meal meal = new Meal();
        meal.setDietPlan(plan);
        meal.setMealType(MealType.BREAKFAST);
        meal.setMealName("Oats");
        meal.setCalories(new BigDecimal("500"));
        meal.setProtein(new BigDecimal("30"));
        meal.setCarbs(new BigDecimal("60"));
        meal.setFat(new BigDecimal("15"));
        meal.setIngredients(List.of("oats", "milk"));
        mealRepository.save(meal);

        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/diet-plans/daily/" + today).header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meals.length()").value(1))
                .andExpect(jsonPath("$.data.meals[0].mealName").value("Oats"));
        mockMvc.perform(get("/api/diet-plans/current").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meals.length()").value(1));
        mockMvc.perform(get("/api/diet-plans/range")
                        .param("startDate", today).param("endDate", today)
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].meals[0].ingredients[1]").value("milk"));
    }
}
