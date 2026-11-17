package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DailyNutritionSummary;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.repo.DailyNutritionSummaryRepository;
import com.bytehealers.healverse.repo.ExerciseLogRepository;
import com.bytehealers.healverse.repo.FoodLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.bytehealers.healverse.repo.WaterLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reading or syncing a day's summary must never generate a diet plan (a slow AI call): it uses
 * the stored plan if there is one and the profile-based targets otherwise.
 */
class NutritionSyncServiceTests {

    private static final Long USER_ID = 5L;
    private static final LocalDate DATE = LocalDate.of(2026, 1, 2);

    private DailyNutritionSummaryRepository summaryRepository;
    private DietPlanService dietPlanService;
    private NutritionCalculatorService calculator;
    private NutritionSyncService service;

    @BeforeEach
    void setUp() {
        summaryRepository = mock(DailyNutritionSummaryRepository.class);
        ExerciseLogRepository exerciseRepository = mock(ExerciseLogRepository.class);
        WaterLogRepository waterRepository = mock(WaterLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FoodLogRepository foodRepository = mock(FoodLogRepository.class);
        dietPlanService = mock(DietPlanService.class);
        calculator = mock(NutritionCalculatorService.class);

        User user = new User();
        user.setId(USER_ID);
        user.setProfile(new UserProfile());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(summaryRepository.findByUserIdAndDate(USER_ID, DATE)).thenReturn(Optional.empty());
        when(summaryRepository.save(any(DailyNutritionSummary.class))).thenAnswer(call -> call.getArgument(0));
        when(foodRepository.findFoodLogsByUserIdAndDateRange(anyLong(), any(), any())).thenReturn(List.of());
        when(exerciseRepository.findByUserIdAndLoggedAtBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(waterRepository.findByUserIdAndLoggedAtBetween(anyLong(), any(), any())).thenReturn(List.of());

        service = new NutritionSyncService(summaryRepository, exerciseRepository, waterRepository,
                userRepository, foodRepository, dietPlanService, calculator);
    }

    private DailyNutritionSummary savedSummary() {
        ArgumentCaptor<DailyNutritionSummary> captor = ArgumentCaptor.forClass(DailyNutritionSummary.class);
        verify(summaryRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void dayWithoutPlanUsesProfileTargetsAndDoesNotGenerateAPlan() {
        when(dietPlanService.findExistingPlan(USER_ID, DATE)).thenReturn(Optional.empty());
        when(calculator.calculateTargetCalories(any())).thenReturn(new BigDecimal("2000"));
        when(calculator.calculateMacros(any(), any())).thenReturn(
                new NutritionCalculatorService.MacroDistribution(
                        new BigDecimal("150"), new BigDecimal("200"), new BigDecimal("60")));

        service.syncDailySummary(USER_ID, DATE);

        verify(dietPlanService, never()).getDailyPlan(anyLong(), any());
        verify(dietPlanService, never()).generateDailyPlan(anyLong(), any());
        assertEquals(new BigDecimal("2000"), savedSummary().getTargetCalories());
    }

    @Test
    void dayWithPlanUsesThePlanTotals() {
        DietPlan plan = new DietPlan();
        plan.setTotalCalories(new BigDecimal("1800"));
        plan.setTotalProtein(new BigDecimal("120"));
        plan.setTotalCarbs(new BigDecimal("180"));
        plan.setTotalFat(new BigDecimal("50"));
        when(dietPlanService.findExistingPlan(USER_ID, DATE)).thenReturn(Optional.of(plan));

        service.syncDailySummary(USER_ID, DATE);

        verify(dietPlanService, never()).getDailyPlan(anyLong(), any());
        assertEquals(new BigDecimal("1800"), savedSummary().getTargetCalories());
    }
}
