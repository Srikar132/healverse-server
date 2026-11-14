package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.LogExerciseRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.DashboardResponse;
import com.bytehealers.healverse.model.ExerciseIntensity;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.service.CalorieCalculatorService;
import com.bytehealers.healverse.service.ExerciseLoggingService;
import com.bytehealers.healverse.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exercise-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExerciseLoggingController {

    private final ExerciseLoggingService exerciseLoggingService;
    private final CalorieCalculatorService calorieCalculatorService;


    @PostMapping("/log")
    public ResponseEntity<ApiResponse<ExerciseLog>> logExercise(@Valid @RequestBody LogExerciseRequest request, @CurrentUserId Long userId) {
        ExerciseLog exerciseLog = exerciseLoggingService.logExercise(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Exercise logged successfully", exerciseLog));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<ExerciseLog>>> getTodaysExerciseLogs(@CurrentUserId Long userId) {
        List<ExerciseLog> exerciseLogs = exerciseLoggingService.getTodaysExerciseLogs(userId);
        return ResponseEntity.ok(ApiResponse.success("Today's exercise logs retrieved successfully", exerciseLogs));
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<List<ExerciseLog>>> getExerciseByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @CurrentUserId Long userId
    ) {
        List<ExerciseLog> exerciseLogs = exerciseLoggingService.getExerciseLogsByDate(userId , date);

        return ResponseEntity.ok(ApiResponse.success(
                "Exercise retrieved successfully",
                exerciseLogs
        ));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ExerciseLog>>> getExerciseLogsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate, @CurrentUserId Long userId) {
        List<ExerciseLog> exerciseLogs = exerciseLoggingService.getExerciseLogsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Exercise logs retrieved successfully", exerciseLogs));
    }

    @PutMapping("/{exerciseLogId}")
    public ResponseEntity<ApiResponse<ExerciseLog>> updateExerciseLog(
            @PathVariable Long exerciseLogId,
            @Valid @RequestBody LogExerciseRequest request, @CurrentUserId Long userId) {
        ExerciseLog updatedLog = exerciseLoggingService.updateExerciseLog(exerciseLogId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Exercise log updated successfully", updatedLog));
    }

    @DeleteMapping("/{exerciseLogId}")
    public ResponseEntity<ApiResponse<String>> deleteExerciseLog(@PathVariable Long exerciseLogId, @CurrentUserId Long userId) {
        boolean deleted = exerciseLoggingService.deleteExerciseLog(exerciseLogId, userId);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Exercise log deleted successfully", "Deleted"));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error("Exercise log not found"));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Map<String, Map<ExerciseIntensity, BigDecimal>>>> getExerciseTypes() {
        Map<String, Map<ExerciseIntensity, BigDecimal>> exerciseTypes = calorieCalculatorService.getAllExerciseTypes();
        return ResponseEntity.ok(ApiResponse.success("Exercise types retrieved successfully", exerciseTypes));
    }
}
