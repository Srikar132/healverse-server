package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.MealType;
import com.bytehealers.healverse.service.DietPlanService;
import com.bytehealers.healverse.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diet-plans")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DietPlanController {

    private final DietPlanService dietPlanService;


    @GetMapping("/weekly/{date}")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getWeeklyPlans(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @CurrentUserId Long userId) {
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Weekly plans retrieved successfully", weeklyPlans));
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<ApiResponse<DietPlan>> getDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @CurrentUserId Long userId) {
        DietPlan dailyPlan = dietPlanService.getDailyPlan(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Daily plan retrieved successfully", dailyPlan));
    }

    @PostMapping("/generate/{date}")
    public ResponseEntity<ApiResponse<DietPlan>> generateDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @CurrentUserId Long userId) {
        DietPlan newPlan = dietPlanService.generateDailyPlan(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Daily plan generated successfully", newPlan));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<DietPlan>> getCurrentDietPlan(@CurrentUserId Long userId) {
        DietPlan currentPlan = dietPlanService.getCurrentDietPlan(userId);
        return ResponseEntity.ok(ApiResponse.success("Current diet plan retrieved successfully", currentPlan));
    }

    @GetMapping("/exists/{date}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> hasDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @CurrentUserId Long userId) {
        boolean exists = dietPlanService.hasDietPlan(userId, date);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }

    @DeleteMapping("/delete/{date}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @CurrentUserId Long userId) {
        dietPlanService.deleteDietPlan(userId, date);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Diet plan deleted successfully")));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getDietPlansByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @CurrentUserId Long userId) {
        List<DietPlan> plans = dietPlanService.getDietPlansByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
    }

    @PostMapping("/meals/generate")
    public ResponseEntity<ApiResponse<Meal>> generateSingleMeal(
            @RequestParam MealType mealType,
            @RequestParam BigDecimal targetCalories, @CurrentUserId Long userId) {
        Meal meal = dietPlanService.generateSingleMeal(userId, mealType, targetCalories);
        return ResponseEntity.ok(ApiResponse.success("Meal generated successfully", meal));
    }

    @PutMapping("/{dietPlanId}/meals/{mealType}/regenerate")
    public ResponseEntity<ApiResponse<DietPlan>> regenerateMeal(
            @CurrentUserId Long userId,
            @PathVariable Long dietPlanId,
            @PathVariable MealType mealType) {
        DietPlan updatedPlan = dietPlanService.regenerateMeal(userId, dietPlanId, mealType);
        return ResponseEntity.ok(ApiResponse.success("Meal regenerated successfully", updatedPlan));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDietPlanSummary(@CurrentUserId Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        List<DietPlan> monthlyPlans = dietPlanService.getDietPlansByDateRange(userId, startOfMonth, endOfMonth);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, now);
        Map<String, Object> summary = Map.of(
                "currentMonth", Map.of(
                        "count", monthlyPlans.size(),
                        "startDate", startOfMonth,
                        "endDate", endOfMonth
                ),
                "currentWeek", Map.of(
                        "count", weeklyPlans.size(),
                        "plans", weeklyPlans
                ),
                "hasCurrentPlan", dietPlanService.hasDietPlan(userId, now)
        );
        return ResponseEntity.ok(ApiResponse.success("Diet plan summary retrieved successfully", summary));
    }

    @PutMapping("/{dietPlanId}/regenerate")
    public ResponseEntity<ApiResponse<DietPlan>> regenerateAllMeals(
            @CurrentUserId Long userId,
            @PathVariable Long dietPlanId) {
        DietPlan updatedPlan = dietPlanService.regenerateAllMeals(userId, dietPlanId);
        return ResponseEntity.ok(ApiResponse.success("All meals regenerated successfully", updatedPlan));
    }

}
