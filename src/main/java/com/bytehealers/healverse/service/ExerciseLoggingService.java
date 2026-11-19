package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.*;
import com.bytehealers.healverse.dto.request.LogExerciseRequest;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.repo.ExerciseLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExerciseLoggingService {

    private final ExerciseLogRepository exerciseLogRepository;

    private final CalorieCalculatorService calorieCalculatorService;

    private final NutritionSyncService nutritionSyncService;

    private final Clock clock;

    private final UserTime userTime;

    private final UserRepository userRepository; // 🔥 Added to fetch User by username

    public ExerciseLog logExercise(LogExerciseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal caloriesBurned = calorieCalculatorService.calculateExerciseCalories(
                request.getExerciseName(),
                request.getDurationMinutes(),
                request.getIntensity()
        );

        ExerciseLog exerciseLog = new ExerciseLog(
                null,
                user,
                request.getExerciseName(),
                request.getDurationMinutes(),
                request.getIntensity(),
                caloriesBurned,
                request.getLoggedAt() != null ? request.getLoggedAt() : LocalDateTime.now(clock),
                null
        );

        ExerciseLog savedLog = exerciseLogRepository.save(exerciseLog);

        // Trigger sync — best-effort only, must never fail the log itself
        syncNutritionSummary(user.getId(), savedLog.getLoggedAt());

        return savedLog;
    }

    // The nutrition-summary sync is a side-effect, not the point of this
    // request — a failure in it (e.g. AI diet-plan generation erroring) must
    // never turn a successful log into an error response.
    private void syncNutritionSummary(Long userId, LocalDateTime loggedAt) {
        try {
            nutritionSyncService.syncAfterExerciseLog(userId, loggedAt);
        } catch (Exception e) {
            log.warn("Nutrition summary sync failed after exercise log for userId {}: {}", userId, e.getMessage());
        }
    }

    public List<ExerciseLog> getTodaysExerciseLogs(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return getExerciseLogsByDate(user.getId(), userTime.today(userId));
    }

    public List<ExerciseLog> getExerciseLogsByDate(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserTime.DayRange day = userTime.dayRange(userId, date);
        return exerciseLogRepository.findByUserIdAndLoggedAtBetween(user.getId(), day.start(), day.endInclusive());
    }


    public List<ExerciseLog> getExerciseLogsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserTime.DayRange range = userTime.range(userId, startDate, endDate);

        return exerciseLogRepository.findByUserIdAndLoggedAtBetween(user.getId(), range.start(), range.endInclusive());
    }

    public ExerciseLog updateExerciseLog(Long exerciseLogId, LogExerciseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Owner-scoped lookup: another user's log is indistinguishable from a missing one
        ExerciseLog exerciseLog = exerciseLogRepository.findByIdAndUser_Id(exerciseLogId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise log not found"));

        BigDecimal caloriesBurned = calorieCalculatorService.calculateExerciseCalories(
                request.getExerciseName(),
                request.getDurationMinutes(),
                request.getIntensity()
        );

        // Update the log
        exerciseLog.setExerciseName(request.getExerciseName());
        exerciseLog.setDurationMinutes(request.getDurationMinutes());
        exerciseLog.setIntensity(request.getIntensity());
        exerciseLog.setCaloriesBurned(caloriesBurned);

        ExerciseLog savedLog = exerciseLogRepository.save(exerciseLog);

        syncNutritionSummary(user.getId(), savedLog.getLoggedAt());

        return savedLog;
    }

    public boolean deleteExerciseLog(Long exerciseLogId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ExerciseLog exerciseLog = exerciseLogRepository.findByIdAndUser_Id(exerciseLogId, user.getId())
                .orElse(null);

        if (exerciseLog == null) {
            return false;
        }

        LocalDateTime loggedAt = exerciseLog.getLoggedAt();
        exerciseLogRepository.delete(exerciseLog);

        syncNutritionSummary(user.getId(), loggedAt);

        return true;
    }
}
