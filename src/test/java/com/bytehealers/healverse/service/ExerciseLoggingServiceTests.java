package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.LogExerciseRequest;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.enums.ExerciseIntensity;
import com.bytehealers.healverse.repo.ExerciseLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExerciseLoggingServiceTests {

    private static final Long USER_ID = 5L;
    private static final Long OTHER_USER_ID = 6L;
    private static final Long LOG_ID = 21L;
    private static final LocalDateTime LOGGED_AT = LocalDateTime.of(2026, 1, 2, 18, 0);

    private ExerciseLogRepository exerciseLogRepository;
    private CalorieCalculatorService calorieCalculatorService;
    private NutritionSyncService nutritionSyncService;
    private UserRepository userRepository;
    private ExerciseLoggingService service;

    @BeforeEach
    void setUp() {
        exerciseLogRepository = mock(ExerciseLogRepository.class);
        calorieCalculatorService = mock(CalorieCalculatorService.class);
        nutritionSyncService = mock(NutritionSyncService.class);
        userRepository = mock(UserRepository.class);
        service = new ExerciseLoggingService(exerciseLogRepository, calorieCalculatorService, nutritionSyncService,
                Clock.systemUTC(), mock(UserTime.class), userRepository);

        stubUser(USER_ID);
        stubUser(OTHER_USER_ID);
        when(exerciseLogRepository.save(any(ExerciseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubUser(Long id) {
        User user = new User();
        user.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    private ExerciseLog existingLog() {
        return new ExerciseLog(LOG_ID, new User(), "Running", 30, ExerciseIntensity.LOW,
                new BigDecimal("100"), LOGGED_AT, LOGGED_AT);
    }

    private LogExerciseRequest request() {
        LogExerciseRequest request = new LogExerciseRequest();
        request.setExerciseName("Cycling");
        request.setDurationMinutes(45);
        request.setIntensity(ExerciseIntensity.HIGH);
        return request;
    }

    @Test
    void updateRecalculatesCaloriesAndSyncsTheLoggedDay() {
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));
        when(calorieCalculatorService.calculateExerciseCalories("Cycling", 45, ExerciseIntensity.HIGH))
                .thenReturn(new BigDecimal("420"));

        ExerciseLog updated = service.updateExerciseLog(LOG_ID, request(), USER_ID);

        assertEquals("Cycling", updated.getExerciseName());
        assertEquals(45, updated.getDurationMinutes());
        assertEquals(ExerciseIntensity.HIGH, updated.getIntensity());
        assertEquals(new BigDecimal("420"), updated.getCaloriesBurned());
        assertEquals(LOGGED_AT, updated.getLoggedAt());
        verify(exerciseLogRepository).save(updated);
        verify(nutritionSyncService).syncAfterExerciseLog(USER_ID, LOGGED_AT);
    }

    @Test
    void updateOfAnotherUsersLogLooksLikeAMissingOne() {
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateExerciseLog(LOG_ID, request(), OTHER_USER_ID));

        verify(exerciseLogRepository, never()).save(any());
        verify(nutritionSyncService, never()).syncAfterExerciseLog(any(), any());
    }

    @Test
    void updateSucceedsEvenWhenTheSummarySyncFails() {
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));
        when(calorieCalculatorService.calculateExerciseCalories("Cycling", 45, ExerciseIntensity.HIGH))
                .thenReturn(new BigDecimal("420"));
        doThrow(new IllegalStateException("sync down")).when(nutritionSyncService)
                .syncAfterExerciseLog(USER_ID, LOGGED_AT);

        ExerciseLog updated = service.updateExerciseLog(LOG_ID, request(), USER_ID);

        assertEquals(new BigDecimal("420"), updated.getCaloriesBurned());
    }

    @Test
    void deleteRemovesTheLogAndSyncsTheLoggedDay() {
        ExerciseLog log = existingLog();
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(log));

        assertTrue(service.deleteExerciseLog(LOG_ID, USER_ID));

        verify(exerciseLogRepository).delete(log);
        verify(nutritionSyncService).syncAfterExerciseLog(USER_ID, LOGGED_AT);
    }

    @Test
    void deleteOfAnotherUsersLogDeletesNothing() {
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertFalse(service.deleteExerciseLog(LOG_ID, OTHER_USER_ID));

        verify(exerciseLogRepository, never()).delete(any());
        verify(nutritionSyncService, never()).syncAfterExerciseLog(any(), any());
    }

    @Test
    void deleteSucceedsEvenWhenTheSummarySyncFails() {
        when(exerciseLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));
        doThrow(new IllegalStateException("sync down")).when(nutritionSyncService)
                .syncAfterExerciseLog(USER_ID, LOGGED_AT);

        assertTrue(service.deleteExerciseLog(LOG_ID, USER_ID));
    }
}
