package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.GamificationSummaryDTO;
import com.bytehealers.healverse.model.DailyPoints;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.FoodItem;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.PointsHistory;
import com.bytehealers.healverse.model.UserStreaks;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.model.enums.PointReason;
import com.bytehealers.healverse.repo.DailyPointsRepository;
import com.bytehealers.healverse.repo.PointsHistoryRepository;
import com.bytehealers.healverse.repo.UserStreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GamificationServiceTests {

    private static final Long USER_ID = 5L;
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 2);

    private DailyPointsRepository dailyPointsRepository;
    private PointsHistoryRepository historyRepository;
    private DietPlanService dietPlanService;
    private GamificationService service;
    private DailyPoints todayPoints;
    private UserStreaks streak;

    @BeforeEach
    void setUp() {
        UserStreakRepository streakRepository = mock(UserStreakRepository.class);
        dailyPointsRepository = mock(DailyPointsRepository.class);
        historyRepository = mock(PointsHistoryRepository.class);
        dietPlanService = mock(DietPlanService.class);
        UserTime userTime = mock(UserTime.class);
        when(userTime.today(USER_ID)).thenReturn(TODAY);
        service = new GamificationService(streakRepository, dailyPointsRepository, historyRepository,
                dietPlanService, userTime);

        todayPoints = new DailyPoints();
        streak = new UserStreaks();
        streak.setUserId(USER_ID);
        when(dailyPointsRepository.findByUserIdAndDate(USER_ID, TODAY)).thenReturn(Optional.of(todayPoints));
        when(streakRepository.findByUserId(USER_ID)).thenReturn(Optional.of(streak));
    }

    private void planSuggests(MealType mealType, String mealName) {
        DietPlan plan = new DietPlan();
        Meal meal = new Meal();
        meal.setMealType(mealType);
        meal.setMealName(mealName);
        meal.setDietPlan(plan);
        plan.setMeals(List.of(meal));
        when(dietPlanService.findExistingPlan(USER_ID, TODAY)).thenReturn(Optional.of(plan));
    }

    private static FoodLog logOf(MealType mealType, String mealName) {
        FoodLog foodLog = new FoodLog();
        foodLog.setMealType(mealType);
        foodLog.setMealName(mealName);
        FoodItem item = new FoodItem();
        item.setFoodLog(foodLog);
        foodLog.setItems(new ArrayList<>(List.of(item)));
        return foodLog;
    }

    private void alreadyRewarded(MealType mealType) {
        PointsHistory earlier = new PointsHistory();
        earlier.setReason(PointReason.DIET_FOLLOW.name());
        earlier.setDescription("Followed " + mealType + " plan (85% match)");
        when(historyRepository.findByUserIdAndDateBetween(USER_ID, TODAY, TODAY)).thenReturn(List.of(earlier));
    }

    private PointsHistory awardedHistory() {
        ArgumentCaptor<PointsHistory> captor = ArgumentCaptor.forClass(PointsHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void anExactNameMatchEarnsThirtyPoints() {
        planSuggests(MealType.BREAKFAST, "Oats with milk");

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Oats with milk"));

        PointsHistory history = awardedHistory();
        assertEquals(30, history.getPointsEarned());
        assertEquals(PointReason.DIET_FOLLOW.name(), history.getReason());
        assertEquals(30, streak.getTotalPoints());
        assertEquals(30, todayPoints.getDietPoints());
    }

    @Test
    void aPartialNameMatchEarnsFifteenPoints() {
        planSuggests(MealType.BREAKFAST, "Oats with milk");

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Oats"));

        assertEquals(15, awardedHistory().getPointsEarned());
    }

    @Test
    void aDifferentMealOfTheRightTypeEarnsFivePoints() {
        planSuggests(MealType.BREAKFAST, "Oats with milk");

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Pancakes"));

        assertEquals(5, awardedHistory().getPointsEarned());
    }

    @Test
    void aMealTypeIsRewardedOnlyOncePerDay() {
        planSuggests(MealType.BREAKFAST, "Oats with milk");
        alreadyRewarded(MealType.BREAKFAST);

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Oats with milk"));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void anotherMealTypeIsStillRewardedAfterBreakfast() {
        planSuggests(MealType.LUNCH, "Rice and dal");
        alreadyRewarded(MealType.BREAKFAST);

        service.recordFoodLog(USER_ID, logOf(MealType.LUNCH, "Rice and dal"));

        assertEquals(30, awardedHistory().getPointsEarned());
    }

    @Test
    void noPlanForTodayAwardsNothing() {
        when(dietPlanService.findExistingPlan(USER_ID, TODAY)).thenReturn(Optional.empty());

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Oats"));

        verify(historyRepository, never()).save(any());
        verify(dailyPointsRepository, never()).save(any());
    }

    @Test
    void aPlanWithoutThatMealTypeAwardsNothing() {
        planSuggests(MealType.LUNCH, "Rice and dal");

        service.recordFoodLog(USER_ID, logOf(MealType.BREAKFAST, "Oats"));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void summaryWithoutAPointsRowTodayReportsZero() {
        when(dailyPointsRepository.findByUserIdAndDate(USER_ID, TODAY)).thenReturn(Optional.empty());

        GamificationSummaryDTO summary = service.getUserSummary(USER_ID);

        assertEquals(0, summary.getTodayPoints());
        assertTrue(summary.getRecentActivity().isEmpty());
    }

    @Test
    void summaryReportsTodaysPoints() {
        todayPoints.setTotalPoints(45);

        assertEquals(45, service.getUserSummary(USER_ID).getTodayPoints());
    }
}
