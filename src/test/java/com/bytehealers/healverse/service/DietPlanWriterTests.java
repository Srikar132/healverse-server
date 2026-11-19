package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.model.enums.ActivityLevel;
import com.bytehealers.healverse.model.enums.Gender;
import com.bytehealers.healverse.model.enums.Goal;
import com.bytehealers.healverse.model.enums.MealType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(DietPlanWriter.class)
class DietPlanWriterTests {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

    @Autowired
    private DietPlanWriter writer;

    @Autowired
    private TestEntityManager entityManager;

    // A user with a profile (User and UserProfile point at each other) saved through a lazy
    // reference, as the writer does: generated equals/hashCode must not follow that cycle.
    @Test
    void planOfAUserWithAProfileIsSavedAndSerializable() throws Exception {
        Long userId = persistUserWithProfile();
        entityManager.clear();

        DietPlan saved = writer.save(userId, DAY, planWithMeals());

        assertEquals(3, saved.getMeals().size());
        assertDoesNotThrow(saved::hashCode);
        saved.getMeals().forEach(meal -> assertDoesNotThrow(meal::hashCode));
        assertDoesNotThrow(saved.getUser()::hashCode);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        assertTrue(objectMapper.writeValueAsString(saved).contains("BREAKFAST"));
    }

    private Long persistUserWithProfile() {
        User user = new User();
        user.setUsername("alice");
        user = entityManager.persistAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setGender(Gender.MALE);
        profile.setAge(30);
        profile.setHeightCm(BigDecimal.valueOf(175));
        profile.setCurrentWeightKg(BigDecimal.valueOf(80));
        profile.setTargetWeightKg(BigDecimal.valueOf(75));
        profile.setActivityLevel(ActivityLevel.SEDENTARY);
        profile.setGoal(Goal.LOSE_WEIGHT);
        entityManager.persistAndFlush(profile);
        user.setProfile(profile);
        return user.getId();
    }

    private static DietPlan planWithMeals() {
        DietPlan plan = new DietPlan();
        plan.setTotalCalories(BigDecimal.valueOf(2000));
        plan.setTotalProtein(BigDecimal.valueOf(100));
        plan.setTotalCarbs(BigDecimal.valueOf(250));
        plan.setTotalFat(BigDecimal.valueOf(60));
        List<Meal> meals = new ArrayList<>();
        for (MealType type : new MealType[]{MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER}) {
            Meal meal = new Meal();
            meal.setDietPlan(plan);
            meal.setMealType(type);
            meal.setMealName(type.name());
            meal.setCalories(BigDecimal.valueOf(600));
            meal.setProtein(BigDecimal.valueOf(30));
            meal.setCarbs(BigDecimal.valueOf(80));
            meal.setFat(BigDecimal.valueOf(20));
            meal.setIngredients(List.of("rice"));
            meals.add(meal);
        }
        plan.setMeals(meals);
        return plan;
    }
}
