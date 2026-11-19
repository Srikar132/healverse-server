package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DailyPoints;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.FoodItem;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserStreaks;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.repo.DailyPointsRepository;
import com.bytehealers.healverse.repo.PointsHistoryRepository;
import com.bytehealers.healverse.repo.UserStreakRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression for the StackOverflowError that made food-log points unusable: entities whose
 * relationships point back at each other must not recurse through equals, hashCode or toString.
 */
class GamificationRecursionTests {

    private static final Long USER_ID = 5L;
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 2);

    private static FoodLog foodLogWithBackReferences() {
        FoodLog foodLog = new FoodLog();
        foodLog.setId(1L);
        foodLog.setMealType(MealType.BREAKFAST);
        foodLog.setMealName("Eggs and banana");
        List<FoodItem> items = new ArrayList<>();
        for (String name : List.of("Boiled egg", "Banana")) {
            FoodItem item = new FoodItem();
            item.setName(name);
            item.setFoodLog(foodLog);
            items.add(item);
        }
        foodLog.setItems(items);
        return foodLog;
    }

    private static DietPlan planWithBackReferences() {
        DietPlan plan = new DietPlan();
        plan.setId(2L);
        plan.setUser(new User());
        List<Meal> meals = new ArrayList<>();
        for (MealType type : List.of(MealType.BREAKFAST, MealType.LUNCH)) {
            Meal meal = new Meal();
            meal.setMealType(type);
            meal.setMealName("Eggs and banana");
            meal.setCalories(BigDecimal.TEN);
            meal.setDietPlan(plan);
            meals.add(meal);
        }
        plan.setMeals(meals);
        return plan;
    }

    @Test
    void recordingAFoodLogDoesNotRecurseThroughTheEntityGraph() {
        PointsHistoryRepository historyRepository = mock(PointsHistoryRepository.class);
        DietPlanService dietPlanService = mock(DietPlanService.class);
        UserTime userTime = mock(UserTime.class);
        when(userTime.today(USER_ID)).thenReturn(TODAY);
        DailyPointsRepository dailyPointsRepository = mock(DailyPointsRepository.class);
        UserStreakRepository streakRepository = mock(UserStreakRepository.class);
        when(dailyPointsRepository.findByUserIdAndDate(USER_ID, TODAY)).thenReturn(Optional.of(new DailyPoints()));
        when(streakRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new UserStreaks()));
        when(dietPlanService.findExistingPlan(USER_ID, TODAY)).thenReturn(Optional.of(planWithBackReferences()));
        GamificationService service = new GamificationService(streakRepository,
                dailyPointsRepository, historyRepository, dietPlanService, userTime, mock(GamificationRowCreator.class));
        FoodLog foodLog = foodLogWithBackReferences();

        assertDoesNotThrow(() -> service.recordFoodLog(USER_ID, foodLog));

        verify(historyRepository).save(any());
    }

    @Test
    void foodLogsAndTheirItemsHaveFiniteEqualsHashCodeAndToString() {
        FoodLog first = foodLogWithBackReferences();
        FoodLog second = foodLogWithBackReferences();

        assertDoesNotThrow(first::hashCode);
        assertDoesNotThrow(first::toString);
        assertDoesNotThrow(first.getItems().get(0)::toString);
        assertEquals(first, second);
        assertEquals(first.getItems().get(0), second.getItems().get(0));
    }

    @Test
    void dietPlansAndTheirMealsHaveFiniteEqualsHashCodeAndToString() {
        DietPlan first = planWithBackReferences();
        DietPlan second = planWithBackReferences();

        assertDoesNotThrow(first::hashCode);
        assertDoesNotThrow(first::toString);
        assertDoesNotThrow(first.getMeals().get(0)::toString);
        assertEquals(first, second);
        assertEquals(first.getMeals().get(0), second.getMeals().get(0));
    }
}
