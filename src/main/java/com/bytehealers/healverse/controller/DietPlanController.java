package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.MealType;
import com.bytehealers.healverse.service.DietPlanService;
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

    @GetMapping("/weekly/{date}")
    public ResponseEntity<List<DietPlan>> getWeeklyPlans(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, date);
        return ResponseEntity.ok(weeklyPlans);
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<DietPlan> getDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);

        DietPlan dailyPlan = dietPlanService.getDailyPlan(username, date);
        return ResponseEntity.ok(dailyPlan);
    }

    @PostMapping("/generate/{date}")
    public ResponseEntity<DietPlan> generateDailyPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        DietPlan newPlan = dietPlanService.generateDailyPlan(username, date);
        return ResponseEntity.ok(newPlan);
    }

    /**
     * Get current diet plan (today's plan)
     */
    @GetMapping("/current")
    public ResponseEntity<DietPlan> getCurrentDietPlan(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);
        DietPlan currentPlan = dietPlanService.getCurrentDietPlan(username);
        return ResponseEntity.ok(currentPlan);
    }

    /**
     * Check if user has a diet plan for a specific date
     */
    @GetMapping("/exists/{date}")
    public ResponseEntity<Map<String, Boolean>> hasDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        boolean exists = dietPlanService.hasDietPlan(username, date);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Delete a diet plan for a specific date
     */
    @DeleteMapping("/delete/{date}")
    public ResponseEntity<Map<String, String>> deleteDietPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        dietPlanService.deleteDietPlan(username, date);
        return ResponseEntity.ok(Map.of("message", "Diet plan deleted successfully"));
    }

    /**
     * Get diet plans within a date range
     */
    @GetMapping("/range")
    public ResponseEntity<List<DietPlan>> getDietPlansByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        List<DietPlan> plans = dietPlanService.getDietPlansByDateRange(username, startDate, endDate);
        return ResponseEntity.ok(plans);
    }

    /**
     * Generate a single meal for a specific meal type
     */
    @PostMapping("/meals/generate")
    public ResponseEntity<Meal> generateSingleMeal(
            @RequestParam MealType mealType,
            @RequestParam BigDecimal targetCalories,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        Meal meal = dietPlanService.generateSingleMeal(username, mealType, targetCalories);
        return ResponseEntity.ok(meal);
    }

    /**
     * Regenerate a specific meal in an existing diet plan
     */
    @PutMapping("/{dietPlanId}/meals/{mealType}/regenerate")
    public ResponseEntity<DietPlan> regenerateMeal(
            @PathVariable Long dietPlanId,
            @PathVariable MealType mealType,
            Authentication authentication) {

        DietPlan updatedPlan = dietPlanService.regenerateMeal(dietPlanId, mealType);
        return ResponseEntity.ok(updatedPlan);
    }

    /**
     * Get diet plans for the current week
     */
    @GetMapping("/weekly/current")
    public ResponseEntity<List<DietPlan>> getCurrentWeeklyPlans(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, LocalDate.now());
        return ResponseEntity.ok(weeklyPlans);
    }

    /**
     * Generate diet plans for the entire current week
     */
    @PostMapping("/weekly/generate")
    public ResponseEntity<List<DietPlan>> generateCurrentWeeklyPlans(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, LocalDate.now());
        return ResponseEntity.ok(weeklyPlans);
    }

    /**
     * Get diet plans for the next week
     */
    @GetMapping("/weekly/next")
    public ResponseEntity<List<DietPlan>> getNextWeeklyPlans(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);
        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, nextWeek);
        return ResponseEntity.ok(weeklyPlans);
    }

    /**
     * Get diet plans for the previous week
     */
    @GetMapping("/weekly/previous")
    public ResponseEntity<List<DietPlan>> getPreviousWeeklyPlans(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);
        LocalDate previousWeek = LocalDate.now().minusWeeks(1);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, previousWeek);
        return ResponseEntity.ok(weeklyPlans);
    }

    /**
     * Generate multiple diet plans for a date range
     */
    @PostMapping("/generate/range")
    public ResponseEntity<List<DietPlan>> generateDietPlansForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);
        List<DietPlan> plans = dietPlanService.getDietPlansByDateRange(username, startDate, endDate);

        // This will generate missing plans within the range through the service logic
        return ResponseEntity.ok(plans);
    }

    /**
     * Bulk delete diet plans for a date range
     */
    @DeleteMapping("/delete/range")
    public ResponseEntity<Map<String, String>> deleteDietPlansInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = getUserNameFromAuth(authentication);

        // Delete each day in the range
        LocalDate currentDate = startDate;
        int deletedCount = 0;

        while (!currentDate.isAfter(endDate)) {
            if (dietPlanService.hasDietPlan(username, currentDate)) {
                dietPlanService.deleteDietPlan(username, currentDate);
                deletedCount++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Deleted " + deletedCount + " diet plans",
                "deletedCount", String.valueOf(deletedCount)
        ));
    }

    /**
     * Get summary of user's diet plans (count by month/week)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDietPlanSummary(Authentication authentication) {
        String username = getUserNameFromAuth(authentication);

        // Get current month plans
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        List<DietPlan> monthlyPlans = dietPlanService.getDietPlansByDateRange(username, startOfMonth, endOfMonth);
        List<DietPlan> weeklyPlans = dietPlanService.getWeeklyPlans(username, now);

        return ResponseEntity.ok(Map.of(
                "currentMonth", Map.of(
                        "count", monthlyPlans.size(),
                        "startDate", startOfMonth,
                        "endDate", endOfMonth
                ),
                "currentWeek", Map.of(
                        "count", weeklyPlans.size(),
                        "plans", weeklyPlans
                ),
                "hasCurrentPlan", dietPlanService.hasDietPlan(username, now)
        ));
    }

    /**
     * Regenerate all meals in a diet plan
     */
    @PutMapping("/{dietPlanId}/regenerate")
    public ResponseEntity<DietPlan> regenerateAllMeals(
            @PathVariable Long dietPlanId,
            Authentication authentication) {

        // Regenerate each meal type
        DietPlan updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.BREAKFAST);
        updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.LUNCH);
        updatedPlan = dietPlanService.regenerateMeal(dietPlanId, MealType.DINNER);

        return ResponseEntity.ok(updatedPlan);
    }

    private String getUserNameFromAuth(Authentication authentication) {
        // Extract user ID from JWT token or authentication principal
        // Implementation depends on your security configuration
        return authentication.getName();
    }
}