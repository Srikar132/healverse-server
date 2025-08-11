package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.MealRepository;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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

    public DietPlan generateDailyPlan(Long userId, LocalDate date) {
        try {
            // Check if plan already exists
            Optional<DietPlan> existingPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
            if (existingPlan.isPresent()) {
                log.info("Returning existing diet plan for user: {} on date: {}", userId, date);
                return existingPlan.get();
            }

            // if plan not exits , only generate if date is >= today's datae , other wise return null object
            if (date.isBefore(LocalDate.now())) {
                log.warn("Cannot generate diet plan for past date: {} (today is {})", date, LocalDate.now());
                return null;
            }

            // Get user with profile
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = user.getProfile();

            if (profile == null) {
                profile = userProfileRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("User profile not found"));
                user.setProfile(profile);
            }

            log.info("Generating daily diet plan for user: {} on date: {}", userId, date);

            // Generate complete diet plan using AI service
//            DietPlan aiGeneratedPlan = aiRecommendationService.generateDietPlan(user);

            DietPlan aiGeneratedPlan = getDummyDietPlan(user);

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

        } catch (Exception e) {
            log.error("Failed to generate daily diet plan for user: {} on date: {}", userId, date, e);
            throw new RuntimeException("Failed to generate daily diet plan: " + e.getMessage(), e);
        }
    }

    public List<DietPlan> getWeeklyPlans(Long userId, LocalDate date) {
        try {
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

        } catch (Exception e) {
            log.error("Failed to get weekly diet plans for user: {}", userId, e);
            throw new RuntimeException("Failed to get weekly diet plans: " + e.getMessage(), e);
        }
    }

    public DietPlan getDailyPlan(Long userId, LocalDate date) {
        try {
            log.info("Getting daily diet plan for user: {} on date: {}", userId, date);

            return dietPlanRepository.findByUserIdAndPlanDate(userId, date)
                    .orElseGet(() -> {
                        log.info("No existing plan found, generating new plan for user: {} on date: {}", userId, date);
                        return generateDailyPlan(userId, date);
                    });

        } catch (Exception e) {
            log.error("Failed to get daily diet plan for user: {} on date: {}", userId, date, e);
            throw new RuntimeException("Failed to get daily diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a single meal for a specific meal type
     */
    public Meal generateSingleMeal(Long userId, MealType mealType, BigDecimal targetCalories) {
        try {
            log.info("Generating single {} meal for user: {}", mealType, userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = userProfileRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("User profile not found"));
            }

            return aiRecommendationService.generateSingleMeal(profile, mealType, targetCalories);

        } catch (Exception e) {
            log.error("Failed to generate single meal for user: {}", userId, e);
            throw new RuntimeException("Failed to generate single meal: " + e.getMessage(), e);
        }
    }

    /**
     * Regenerate a specific meal in an existing diet plan
     */
    public DietPlan regenerateMeal(Long dietPlanId, MealType mealType) {
        try {
            log.info("Regenerating {} meal for diet plan: {}", mealType, dietPlanId);

            DietPlan dietPlan = dietPlanRepository.findById(dietPlanId)
                    .orElseThrow(() -> new RuntimeException("Diet plan not found"));

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

            // Save updated plan
            DietPlan savedPlan = dietPlanRepository.save(dietPlan);
            mealRepository.save(newMeal);

            log.info("Successfully regenerated {} meal for diet plan: {}", mealType, dietPlanId);
            return savedPlan;

        } catch (Exception e) {
            log.error("Failed to regenerate meal for diet plan: {}", dietPlanId, e);
            throw new RuntimeException("Failed to regenerate meal: " + e.getMessage(), e);
        }
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
     * Get diet plan statistics for a user
     */
//    public DietPlanStats getDietPlanStats(String username, LocalDate startDate, LocalDate endDate) {
//        try {
//            log.info("Getting diet plan statistics for user: {} from {} to {}", username, startDate, endDate);
//
//            List<DietPlan> plans = dietPlanRepository.findPlansByUsernameAndDateRange(username, startDate, endDate);
//
//            if (plans.isEmpty()) {
//                return new DietPlanStats(); // Return empty stats
//            }
//
//            // Calculate averages
//            BigDecimal avgCalories = plans.stream()
//                    .map(DietPlan::getTotalCalories)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add)
//                    .divide(BigDecimal.valueOf(plans.size()), 2, java.math.RoundingMode.HALF_UP);
//
//            BigDecimal avgProtein = plans.stream()
//                    .map(DietPlan::getTotalProtein)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add)
//                    .divide(BigDecimal.valueOf(plans.size()), 2, java.math.RoundingMode.HALF_UP);
//
//            BigDecimal avgCarbs = plans.stream()
//                    .map(DietPlan::getTotalCarbs)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add)
//                    .divide(BigDecimal.valueOf(plans.size()), 2, java.math.RoundingMode.HALF_UP);
//
//            BigDecimal avgFat = plans.stream()
//                    .map(DietPlan::getTotalFat)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add)
//                    .divide(BigDecimal.valueOf(plans.size()), 2, java.math.RoundingMode.HALF_UP);
//
//            return DietPlanStats.builder()
//                    .totalPlans(plans.size())
//                    .averageCalories(avgCalories)
//                    .averageProtein(avgProtein)
//                    .averageCarbs(avgCarbs)
//                    .averageFat(avgFat)
//                    .dateRange(startDate + " to " + endDate)
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Failed to get diet plan statistics for user: {}", username, e);
//            throw new RuntimeException("Failed to get diet plan statistics: " + e.getMessage(), e);
//        }
//    }

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
        try {
            Optional<DietPlan> dietPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
            if (dietPlan.isPresent()) {
                dietPlanRepository.delete(dietPlan.get());
                log.info("Deleted diet plan for user: {} on date: {}", userId, date);
            } else {
                log.warn("No diet plan found to delete for user: {} on date: {}", userId, date);
            }
        } catch (Exception e) {
            log.error("Failed to delete diet plan for user: {} on date: {}", userId, date, e);
            throw new RuntimeException("Failed to delete diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Get all diet plans for a user within a date range
     */
    public List<DietPlan> getDietPlansByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Getting diet plans for user: {} from {} to {}", userId, startDate, endDate);
            return dietPlanRepository.findWeeklyPlansByUserIdAndDateRange(userId, startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to get diet plans by date range for user: {}", userId, e);
            throw new RuntimeException("Failed to get diet plans by date range: " + e.getMessage(), e);
        }
    }


    // DUMMY DATA
    public DietPlan getDummyDietPlan(User user) {
        // Create dummy meals
        Meal breakfast = new Meal();
        breakfast.setMealType(MealType.BREAKFAST);
        breakfast.setMealName("Oats with Banana");
        breakfast.setCalories(new BigDecimal("350.00"));
        breakfast.setProtein(new BigDecimal("10.00"));
        breakfast.setCarbs(new BigDecimal("60.00"));
        breakfast.setFat(new BigDecimal("5.00"));
        breakfast.setPreparationTimeMinutes(10);
        breakfast.setInstructions("Mix oats with milk and top with sliced banana.");
        breakfast.setHealthBenefits("Provides energy and improves digestion.");
        breakfast.setCreatedAt(LocalDateTime.now());
        breakfast.setIngredients(List.of("Oats", "Milk", "Banana", "Honey"));

        Meal lunch = new Meal();
        lunch.setMealType(MealType.LUNCH);
        lunch.setMealName("Grilled Chicken with Rice");
        lunch.setCalories(new BigDecimal("600.00"));
        lunch.setProtein(new BigDecimal("45.00"));
        lunch.setCarbs(new BigDecimal("50.00"));
        lunch.setFat(new BigDecimal("15.00"));
        lunch.setPreparationTimeMinutes(30);
        lunch.setInstructions("Grill the chicken and serve with boiled rice.");
        lunch.setHealthBenefits("High protein meal for muscle recovery.");
        lunch.setCreatedAt(LocalDateTime.now());
        lunch.setIngredients(List.of("Chicken", "Rice", "Spices", "Olive Oil"));

        // Create DietPlan
        DietPlan dietPlan = new DietPlan();
        dietPlan.setUser(user);
        dietPlan.setPlanDate(LocalDate.now());
        dietPlan.setTotalCalories(new BigDecimal("950.00"));
        dietPlan.setTotalProtein(new BigDecimal("55.00"));
        dietPlan.setTotalCarbs(new BigDecimal("110.00"));
        dietPlan.setTotalFat(new BigDecimal("20.00"));
        dietPlan.setIsGenerated(true);
        dietPlan.setCreatedAt(LocalDateTime.now());

        // Link meals to the diet plan
        breakfast.setDietPlan(dietPlan);
        lunch.setDietPlan(dietPlan);
        dietPlan.setMeals(List.of(breakfast, lunch));

        return dietPlan;
    }

}
