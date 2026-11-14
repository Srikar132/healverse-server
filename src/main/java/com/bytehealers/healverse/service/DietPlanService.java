package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
@Transactional
@Slf4j
public class DietPlanService {
    @Autowired
    private DietPlanRepository dietPlanRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AIRecommendationService aiRecommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private DailyNutritionSummaryRepository dailyNutritionSummaryRepository;

    /**
     * Generate nutritional adjustment context based on historical data
     */

    /**
     * Enhanced prompt that includes historical data context
     */

    // Placeholder methods - implement based on your business logic

    @Transactional
    public DietPlan generateDailyPlan(Long userId, LocalDate date) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Check if plan already exists
        Optional<DietPlan> existingPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
        if (existingPlan.isPresent()) {
            log.info("Returning existing diet plan for user: {} on date: {}", userId, date);
            return existingPlan.get(); // Safe - checked with isPresent()
        }

        UserProfile profile = user.getProfile();

        if (profile == null) {
            profile = userProfileRepository.findByUser(user)
                    .orElseThrow(() -> new BadRequestException("Complete your health profile before generating a diet plan"));
            user.setProfile(profile);
        }

        log.info("Generating daily diet plan for user: {} on date: {}", userId, date);

        // Generate complete diet plan using AI service
        DietPlan aiGeneratedPlan = aiRecommendationService.generateDietPlan(user);

        // Update the plan with the specific date
        aiGeneratedPlan.setPlanDate(date);
        aiGeneratedPlan.setUser(user);

        // Save the diet plan
        DietPlan savedPlan = dietPlanRepository.save(aiGeneratedPlan);

        // Save meals with proper diet plan reference
        if (aiGeneratedPlan.getMeals() != null) {
            for (Meal meal : aiGeneratedPlan.getMeals()) {
                meal.setDietPlan(savedPlan);
                mealRepository.save(meal);
            }
        }

        log.info("Successfully generated and saved diet plan with {} meals for user: {} on date: {}",
                aiGeneratedPlan.getMeals().size(), userId, date);

        return savedPlan;

    }

    public List<DietPlan> getWeeklyPlans(Long userId, LocalDate date) {
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        log.info("Getting weekly diet plans for user: {} from {} to {}", userId, startOfWeek, endOfWeek);

        List<DietPlan> existingPlans = dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(userId, startOfWeek, endOfWeek);

        // Generate missing plans for the week
        for (LocalDate currentDate = startOfWeek; !currentDate.isAfter(endOfWeek); currentDate = currentDate.plusDays(1)) {
            final LocalDate dateToCheck = currentDate;
            boolean planExists = existingPlans.stream()
                    .anyMatch(plan -> plan.getPlanDate().equals(dateToCheck));

            if (!planExists) {
                try {
                    DietPlan newPlan = generateDailyPlan(userId, currentDate);
                    existingPlans.add(newPlan);
                } catch (Exception e) {
                    log.warn("Failed to generate diet plan for date: {} for user: {}", currentDate, userId, e);
                    // Continue with other dates even if one fails

                }
            }
        }

        List<DietPlan> sortedPlans = existingPlans.stream()
                .sorted((p1, p2) -> p1.getPlanDate().compareTo(p2.getPlanDate()))
                .toList();

        log.info("Retrieved {} diet plans for weekly view for user: {}", sortedPlans.size(), userId);
        return sortedPlans;

    }

    // REQUIRES_NEW: this is called from NutritionSyncService while a log-food/
    // water/exercise transaction is already active. If AI generation fails here
    // (e.g. invalid OpenAI key), only THIS isolated transaction rolls back —
    // without REQUIRES_NEW, the failure would mark the caller's shared
    // transaction rollback-only, causing an UnexpectedRollbackException at
    // commit time that fails the unrelated logging request too.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietPlan getDailyPlan(Long userId, LocalDate date) {
        log.info("Getting daily diet plan for user: {} on date: {}", userId, date);

        return dietPlanRepository.findByUserIdAndPlanDate(userId, date)
                .orElseGet(() -> {
                    log.info("No existing plan found, generating new plan for user: {} on date: {}", userId, date);
                    return generateDailyPlan(userId, date);
                });

    }

    /**
     * Generate a single meal for a specific meal type
     */
    @Transactional
    public Meal generateSingleMeal(Long userId, MealType mealType, BigDecimal targetCalories) {
        log.info("Generating single {} meal for user: {}", mealType, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = userProfileRepository.findByUser(user)
                    .orElseThrow(() -> new BadRequestException("Complete your health profile before generating a diet plan"));
        }

        return aiRecommendationService.generateSingleMeal(profile, mealType, targetCalories);

    }

    /**
     * Regenerate a specific meal in an existing diet plan
     */
    @Transactional
    public DietPlan regenerateAllMeals(Long userId, Long dietPlanId) {
        log.info("Regenerating all meals for diet plan: {}", dietPlanId);

        DietPlan dietPlan = dietPlanRepository.findById(dietPlanId)
                .filter(plan -> plan.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Diet plan not found"));

        // Remove all existing meals
        List<Meal> meals = dietPlan.getMeals();
        meals.clear();

        // Generate new meals for each type
        List<MealType> mealTypes = Arrays.asList(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);

        for (MealType mealType : mealTypes) {
            BigDecimal mealCalories = calculateMealCalories(dietPlan.getTotalCalories(), mealType);
            Meal newMeal = generateSingleMeal(dietPlan.getUser().getId(), mealType, mealCalories);
            newMeal.setDietPlan(dietPlan);
            meals.add(newMeal);
        }

        // Recalculate totals
        recalculateDietPlanTotals(dietPlan);

        // Save updated plan (should cascade to save meals)
        DietPlan savedPlan = dietPlanRepository.save(dietPlan);

        log.info("Successfully regenerated all meals for diet plan: {}", dietPlanId);
        return savedPlan;

    }

    // Updated single meal regeneration method
    @Transactional
    public DietPlan regenerateMeal(Long userId, Long dietPlanId, MealType mealType) {
        log.info("Regenerating {} meal for diet plan: {}", mealType, dietPlanId);

        DietPlan dietPlan = dietPlanRepository.findById(dietPlanId)
                .filter(plan -> plan.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Diet plan not found"));

        // Calculate target calories for this meal type
        BigDecimal mealCalories = calculateMealCalories(dietPlan.getTotalCalories(), mealType);

        // Generate new meal
        Meal newMeal = generateSingleMeal(dietPlan.getUser().getId(), mealType, mealCalories);

        // Replace the existing meal
        List<Meal> meals = dietPlan.getMeals();
        meals.removeIf(meal -> meal.getMealType() == mealType);

        newMeal.setDietPlan(dietPlan);
        meals.add(newMeal);

        // Recalculate totals
        recalculateDietPlanTotals(dietPlan);

        // Save updated plan (cascade should handle meal saving)
        DietPlan savedPlan = dietPlanRepository.save(dietPlan);

        log.info("Successfully regenerated {} meal for diet plan: {}", mealType, dietPlanId);
        return savedPlan;

    }
    /**
     * Calculate target calories for a specific meal type
     */
    private BigDecimal calculateMealCalories(BigDecimal totalCalories, MealType mealType) {
        return switch (mealType) {
            case BREAKFAST -> totalCalories.multiply(BigDecimal.valueOf(0.25));
            case LUNCH -> totalCalories.multiply(BigDecimal.valueOf(0.40));
            case DINNER -> totalCalories.multiply(BigDecimal.valueOf(0.35));
            case SNACK -> totalCalories.multiply(BigDecimal.valueOf(0.10));
        };
    }

    /**
     * Recalculate diet plan totals from meals
     */
    private void recalculateDietPlanTotals(DietPlan dietPlan) {
        List<Meal> meals = dietPlan.getMeals();

        BigDecimal totalCalories = meals.stream()
                .map(Meal::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProtein = meals.stream()
                .map(Meal::getProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCarbs = meals.stream()
                .map(Meal::getCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFat = meals.stream()
                .map(Meal::getFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dietPlan.setTotalCalories(totalCalories);
        dietPlan.setTotalProtein(totalProtein);
        dietPlan.setTotalCarbs(totalCarbs);
        dietPlan.setTotalFat(totalFat);
    }

    /**
     * Get user's current diet plan (today's plan)
     */
    public DietPlan getCurrentDietPlan(Long userId) {
        return getDailyPlan(userId, LocalDate.now());
    }

    /**
     * Check if user has a diet plan for a specific date
     */
    public boolean hasDietPlan(Long userId, LocalDate date) {
        return dietPlanRepository.findByUserIdAndPlanDate(userId, date).isPresent();
    }

    /**
     * Delete a diet plan for a specific date
     */
    public void deleteDietPlan(Long userId, LocalDate date) {
        Optional<DietPlan> dietPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
        if (dietPlan.isPresent()) {
            dietPlanRepository.delete(dietPlan.get());
            log.info("Deleted diet plan for user: {} on date: {}", userId, date);
        } else {
            log.warn("No diet plan found to delete for user: {} on date: {}", userId, date);
        }
    }

    /**
     * Get all diet plans for a user within a date range
     */
    public List<DietPlan> getDietPlansByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting diet plans for user: {} from {} to {}", userId, startDate, endDate);
        return dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(userId, startDate, endDate);
    }

    public DietPlan getTodaysDietPlan(Long userId) {
        return getDailyPlan(userId, LocalDate.now());
    }
    

}
