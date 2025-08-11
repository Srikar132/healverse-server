package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.MealType;
import com.bytehealers.healverse.service.DietPlanService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DietPlanController {

    @Autowired
    private DietPlanService dietPlanService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/weekly/{date}")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getWeeklyPlans(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Weekly plans retrieved successfully", weeklyPlans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get weekly plans: " + e.getMessage()));
        }
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<ApiResponse<DietPlan>> getDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            DietPlan dailyPlan = dietPlanService.getDailyPlan(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Daily plan retrieved successfully", dailyPlan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get daily plan: " + e.getMessage()));
        }
    }

    @PostMapping("/generate/{date}")
    public ResponseEntity<ApiResponse<DietPlan>> generateDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            DietPlan newPlan = dietPlanService.generateDailyPlan(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Daily plan generated successfully", newPlan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to generate daily plan: " + e.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<DietPlan>> getCurrentDietPlan() {
        try {
            Long userId = userContext.getCurrentUserId();
            DietPlan currentPlan = dietPlanService.getCurrentDietPlan(userId);
            return ResponseEntity.ok(ApiResponse.success("Current diet plan retrieved successfully", currentPlan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get current diet plan: " + e.getMessage()));
        }
    }

    @GetMapping("/exists/{date}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> hasDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            boolean exists = dietPlanService.hasDietPlan(userId, date);
            return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to check diet plan existence: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{date}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            dietPlanService.deleteDietPlan(userId, date);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Diet plan deleted successfully")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete diet plan: " + e.getMessage()));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getDietPlansByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DietPlan> plans = dietPlanService.getDietPlansByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get plans: " + e.getMessage()));
        }
    }

    @PostMapping("/meals/generate")
    public ResponseEntity<ApiResponse<Meal>> generateSingleMeal(
            @RequestParam MealType mealType,
            @RequestParam BigDecimal targetCalories) {
        try {
            Long userId = userContext.getCurrentUserId();
            Meal meal = dietPlanService.generateSingleMeal(userId, mealType, targetCalories);
            return ResponseEntity.ok(ApiResponse.success("Meal generated successfully", meal));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to generate meal: " + e.getMessage()));
        }
    }

    @PutMapping("/{dietPlanId}/meals/{mealType}/regenerate")
    public ResponseEntity<ApiResponse<DietPlan>> regenerateMeal(
            @PathVariable Long dietPlanId,
            @PathVariable MealType mealType) {
        try {
            DietPlan updatedPlan = dietPlanService.regenerateMeal(dietPlanId, mealType);
            return ResponseEntity.ok(ApiResponse.success("Meal regenerated successfully", updatedPlan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to regenerate meal: " + e.getMessage()));
        }
    }

    @GetMapping("/weekly/current")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getCurrentWeeklyPlans() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, LocalDate.now());
            return ResponseEntity.ok(ApiResponse.success("Current weekly plans retrieved successfully", weeklyPlans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get current weekly plans: " + e.getMessage()));
        }
    }

    @PostMapping("/weekly/generate")
    public ResponseEntity<ApiResponse<List<DietPlan>>> generateCurrentWeeklyPlans() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, LocalDate.now());
            return ResponseEntity.ok(ApiResponse.success("Current weekly plans generated successfully", weeklyPlans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to generate weekly plans: " + e.getMessage()));
        }
    }

    @GetMapping("/weekly/next")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getNextWeeklyPlans() {
        try {
            Long userId = userContext.getCurrentUserId();
            LocalDate nextWeek = LocalDate.now().plusWeeks(1);
            List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, nextWeek);
            return ResponseEntity.ok(ApiResponse.success("Next week's plans retrieved successfully", weeklyPlans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get next week's plans: " + e.getMessage()));
        }
    }

    @GetMapping("/weekly/previous")
    public ResponseEntity<ApiResponse<List<DietPlan>>> getPreviousWeeklyPlans() {
        try {
            Long userId = userContext.getCurrentUserId();
            LocalDate previousWeek = LocalDate.now().minusWeeks(1);
            List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(userId, previousWeek);
            return ResponseEntity.ok(ApiResponse.success("Previous week's plans retrieved successfully", weeklyPlans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get previous week's plans: " + e.getMessage()));
        }
    }

    @PostMapping("/generate/range")
    public ResponseEntity<ApiResponse<List<DietPlan>>> generateDietPlansForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DietPlan> plans = dietPlanService.getDietPlansByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Plans for range generated/retrieved successfully", plans));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to generate plans for range: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/range")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteDietPlansInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            LocalDate currentDate = startDate;
            int deletedCount = 0;
            while (!currentDate.isAfter(endDate)) {
                if (dietPlanService.hasDietPlan(userId, currentDate)) {
                    dietPlanService.deleteDietPlan(userId, currentDate);
                    deletedCount++;
                }
                currentDate = currentDate.plusDays(1);
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "message", "Deleted " + deletedCount + " diet plans",
                    "deletedCount", String.valueOf(deletedCount)
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete plans in range: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDietPlanSummary() {
        try {
            Long userId = userContext.getCurrentUserId();
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get diet plan summary: " + e.getMessage()));
        }
    }

    @PutMapping("/{dietPlanId}/regenerate")
    public ResponseEntity<ApiResponse<DietPlan>> regenerateAllMeals(
            @PathVariable Long dietPlanId) {
        try {
            DietPlan updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.BREAKFAST);
            updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.LUNCH);
            updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.DINNER);
            return ResponseEntity.ok(ApiResponse.success("All meals regenerated successfully", updatedPlan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to regenerate all meals: " + e.getMessage()));
        }
    }
}
