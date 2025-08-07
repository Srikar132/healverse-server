package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.LogExerciseRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.model.ExerciseIntensity;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.service.CalorieCalculatorService;
import com.bytehealers.healverse.service.ExerciseLoggingService;
import com.bytehealers.healverse.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exercise-logs")
public class ExerciseLoggingController {

    @Autowired
    private ExerciseLoggingService exerciseLoggingService;

    @Autowired
    private CalorieCalculatorService calorieCalculatorService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<ExerciseLog>> logExercise(@Valid @RequestBody LogExerciseRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            ExerciseLog exerciseLog = exerciseLoggingService.logExercise(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Exercise logged successfully", exerciseLog));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to log exercise: " + e.getMessage()));
        }
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<ExerciseLog>>> getTodaysExerciseLogs() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<ExerciseLog> exerciseLogs = exerciseLoggingService.getTodaysExerciseLogs(userId);
            return ResponseEntity.ok(ApiResponse.success("Today's exercise logs retrieved successfully", exerciseLogs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve exercise logs: " + e.getMessage()));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ExerciseLog>>> getExerciseLogsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<ExerciseLog> exerciseLogs = exerciseLoggingService.getExerciseLogsByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Exercise logs retrieved successfully", exerciseLogs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve exercise logs: " + e.getMessage()));
        }
    }

    @PutMapping("/{exerciseLogId}")
    public ResponseEntity<ApiResponse<ExerciseLog>> updateExerciseLog(
            @PathVariable Long exerciseLogId,
            @Valid @RequestBody LogExerciseRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            ExerciseLog updatedLog = exerciseLoggingService.updateExerciseLog(exerciseLogId, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Exercise log updated successfully", updatedLog));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update exercise log: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{exerciseLogId}")
    public ResponseEntity<ApiResponse<String>> deleteExerciseLog(@PathVariable Long exerciseLogId) {
        try {
            Long userId = userContext.getCurrentUserId();
            boolean deleted = exerciseLoggingService.deleteExerciseLog(exerciseLogId, userId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Exercise log deleted successfully", "Deleted"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete exercise log: " + e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Map<String, Map<ExerciseIntensity, BigDecimal>>>> getExerciseTypes() {
        try {
            Map<String, Map<ExerciseIntensity, BigDecimal>> exerciseTypes = calorieCalculatorService.getAllExerciseTypes();
            return ResponseEntity.ok(ApiResponse.success("Exercise types retrieved successfully", exerciseTypes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve exercise types: " + e.getMessage()));
        }
    }
}