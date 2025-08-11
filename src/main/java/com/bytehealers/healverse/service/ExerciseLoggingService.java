package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.LogExerciseRequest;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.repo.ExerciseLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExerciseLoggingService {

    @Autowired
    private ExerciseLogRepository exerciseLogRepository;

    @Autowired
    private CalorieCalculatorService calorieCalculatorService;



    @Lazy
    @Autowired
    private NutritionSyncService nutritionSyncService;

    @Autowired
    private UserRepository userRepository; // 🔥 Added to fetch User by username

    public ExerciseLog logExercise(LogExerciseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                request.getLoggedAt() != null ? request.getLoggedAt() : LocalDateTime.now(),
                null
        );

        ExerciseLog savedLog = exerciseLogRepository.save(exerciseLog);

        // Trigger sync
        nutritionSyncService.syncAfterExerciseLog(user.getId(), savedLog.getLoggedAt());

        return savedLog;
    }

    public List<ExerciseLog> getTodaysExerciseLogs(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return exerciseLogRepository.findTodaysExerciseLogs(user.getId());
    }

    public List<ExerciseLog> getExerciseLogsByDate(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return exerciseLogRepository.findExerciseLogsByDate(user.getId(), date);
    }


    public List<ExerciseLog> getExerciseLogsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return exerciseLogRepository.findByUserIdAndLoggedAtBetween(user.getId(), startDateTime, endDateTime);
    }

    public ExerciseLog updateExerciseLog(Long exerciseLogId, LogExerciseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExerciseLog exerciseLog = exerciseLogRepository.findById(exerciseLogId)
                .orElseThrow(() -> new RuntimeException("Exercise log not found"));

        // Check if this log belongs to the user
        if (!exerciseLog.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

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

        nutritionSyncService.syncAfterExerciseLog(user.getId(), savedLog.getLoggedAt());

        return savedLog;
    }

    public boolean deleteExerciseLog(Long exerciseLogId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExerciseLog exerciseLog = exerciseLogRepository.findById(exerciseLogId)
                .orElse(null);

        if (exerciseLog == null) {
            return false;
        }

        if (!exerciseLog.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        LocalDateTime loggedAt = exerciseLog.getLoggedAt();
        exerciseLogRepository.delete(exerciseLog);

        nutritionSyncService.syncAfterExerciseLog(user.getId(), loggedAt);

        return true;
    }
}
