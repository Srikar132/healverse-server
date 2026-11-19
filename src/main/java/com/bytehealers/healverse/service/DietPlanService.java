package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.math.BigDecimal;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DietPlanService {
    private final DietPlanRepository dietPlanRepository;

    private final UserProfileRepository userProfileRepository;

    private final AIRecommendationService aiRecommendationService;

    private final UserRepository userRepository;

    private final DailyNutritionSummaryRepository dailyNutritionSummaryRepository;

    private final DietPlanWriter dietPlanWriter;

    private final ConcurrentMap<PlanKey, CompletableFuture<DietPlan>> inFlight = new ConcurrentHashMap<>();

    private final UserTime userTime;

    /**
     * Generate nutritional adjustment context based on historical data
     */

    /**
     * Enhanced prompt that includes historical data context
     */

    // Placeholder methods - implement based on your business logic

    /**
     * Returns the plan for the day, generating it when missing. The AI call (seconds) runs with no
     * transaction, no row lock and no DB connection held; only the final save is transactional.
     * Concurrent calls for the same user and day share one AI call.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DietPlan generateDailyPlan(Long userId, LocalDate date) {
        Optional<DietPlan> existingPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
        if (existingPlan.isPresent()) {
            log.info("Returning existing diet plan for user: {} on date: {}", userId, date);
            return existingPlan.get();
        }
        return generateOnce(new PlanKey(userId, date));
    }

    private DietPlan generateOnce(PlanKey key) {
        CompletableFuture<DietPlan> mine = new CompletableFuture<>();
        CompletableFuture<DietPlan> running = inFlight.putIfAbsent(key, mine);
        if (running != null) {
            log.info("Joining in-flight diet plan generation for user: {} on date: {}", key.userId(), key.date());
            return await(running);
        }
        try {
            DietPlan plan = generateAndStore(key);
            mine.complete(plan);
            return plan;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private DietPlan generateAndStore(PlanKey key) {
        Long userId = key.userId();
        LocalDate date = key.date();

        // A request that finished just before we registered as the owner has already stored the plan.
        Optional<DietPlan> stored = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
        if (stored.isPresent()) {
            return stored.get();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setProfile(userProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Complete your health profile before generating a diet plan")));

        log.info("Generating daily diet plan for user: {} on date: {}", userId, date);
        DietPlan generated = aiRecommendationService.generateDietPlan(user);

        try {
            DietPlan saved = dietPlanWriter.save(userId, date, generated);
            log.info("Saved diet plan with {} meals for user: {} on date: {}", saved.getMeals().size(), userId, date);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Another instance (or a request that bypassed the in-flight map) stored the day first.
            log.info("Diet plan for user: {} on date: {} was stored concurrently, using it", userId, date);
            return dietPlanRepository.findByUserIdAndPlanDate(userId, date).orElseThrow(() -> e);
        }
    }

    private static DietPlan await(CompletableFuture<DietPlan> running) {
        try {
            return running.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    private record PlanKey(Long userId, LocalDate date) {}

    /** The stored plans of the week containing {@code date}, ordered by day. Never generates. */
    @Transactional(readOnly = true)
    public List<DietPlan> getWeeklyPlans(Long userId, LocalDate date) {
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        log.info("Getting weekly diet plans for user: {} from {} to {}", userId, startOfWeek, endOfWeek);
        return dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(userId, startOfWeek, endOfWeek);
    }

    /**
     * The plan stored for a day, or empty. It never generates one, so it is cheap and safe to call
     * from read paths (the daily summary, food logging).
     */
    @Transactional(readOnly = true)
    public Optional<DietPlan> findExistingPlan(Long userId, LocalDate date) {
        return dietPlanRepository.findByUserIdAndPlanDate(userId, date);
    }

    /** The stored plan for a day; throws {@link ResourceNotFoundException} when there is none. */
    @Transactional(readOnly = true)
    public DietPlan getDailyPlan(Long userId, LocalDate date) {
        return findExistingPlan(userId, date)
                .orElseThrow(() -> new ResourceNotFoundException("No diet plan for " + date));
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
        return getDailyPlan(userId, userTime.today(userId));
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

}
