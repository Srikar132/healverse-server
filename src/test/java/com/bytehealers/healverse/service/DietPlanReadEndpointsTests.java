package com.bytehealers.healverse.service;

import com.bytehealers.healverse.controller.DietPlanController;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.repo.DailyNutritionSummaryRepository;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GET endpoints must only read: a missing plan is a 404 / empty list, never an AI call or an insert.
 */
class DietPlanReadEndpointsTests {

    private static final Long USER_ID = 7L;
    private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

    private DietPlanRepository dietPlanRepository;
    private AIRecommendationService aiRecommendationService;
    private DietPlanController controller;

    @BeforeEach
    void setUp() {
        dietPlanRepository = mock(DietPlanRepository.class);
        aiRecommendationService = mock(AIRecommendationService.class);
        DietPlanService service = new DietPlanService(dietPlanRepository, mock(UserProfileRepository.class),
                aiRecommendationService, mock(UserRepository.class), mock(DailyNutritionSummaryRepository.class),
                mock(DietPlanWriter.class));
        controller = new DietPlanController(service);
    }

    @Test
    void dailyReturnsTheStoredPlan() {
        DietPlan plan = new DietPlan();
        when(dietPlanRepository.findByUserIdAndPlanDate(USER_ID, DAY)).thenReturn(Optional.of(plan));

        assertSame(plan, controller.getDailyPlan(DAY, USER_ID).getBody().getData());
    }

    @Test
    void dailyWithoutAPlanIsNotFoundAndDoesNotGenerate() {
        when(dietPlanRepository.findByUserIdAndPlanDate(USER_ID, DAY)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> controller.getDailyPlan(DAY, USER_ID));

        verifyNothingGeneratedOrSaved();
    }

    @Test
    void currentWithoutAPlanIsNotFoundAndDoesNotGenerate() {
        when(dietPlanRepository.findByUserIdAndPlanDate(anyLong(), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> controller.getCurrentDietPlan(USER_ID));

        verifyNothingGeneratedOrSaved();
    }

    @Test
    void weeklyReturnsOnlyStoredPlansAndDoesNotFillTheGaps() {
        DietPlan monday = new DietPlan();
        when(dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(monday));

        List<DietPlan> plans = controller.getWeeklyPlans(DAY, USER_ID).getBody().getData();

        assertEquals(List.of(monday), plans);
        verifyNothingGeneratedOrSaved();
    }

    @Test
    void summaryDoesNotGenerateMissingPlans() {
        when(dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(anyLong(), any(), any())).thenReturn(List.of());
        when(dietPlanRepository.findByUserIdAndPlanDate(anyLong(), any())).thenReturn(Optional.empty());

        var response = controller.getDietPlanSummary(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().getData().get("hasCurrentPlan"));
        assertTrue(response.getBody().isSuccess());
        verifyNothingGeneratedOrSaved();
    }

    private void verifyNothingGeneratedOrSaved() {
        verifyNoInteractions(aiRecommendationService);
        verify(dietPlanRepository, never()).save(any());
        verify(dietPlanRepository, never()).saveAndFlush(any());
    }
}
