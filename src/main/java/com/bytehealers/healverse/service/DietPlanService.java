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

    public DietPlan generateDailyPlan(String username, LocalDate date) {
        try {
            // Check if plan already exists
            Optional<DietPlan> existingPlan = dietPlanRepository.findByUser_UsernameAndPlanDate(username, date);
            if (existingPlan.isPresent()) {
                log.info("Returning existing diet plan for user: {} on date: {}", username, date);
                return existingPlan.get();
            }

            // Get user with profile
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = userProfileRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("User profile not found"));
                user.setProfile(profile);
            }

            log.info("Generating daily diet plan for user: {} on date: {}", username, date);

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
                    aiGeneratedPlan.getMeals().size(), username, date);

            return savedPlan;

        } catch (Exception e) {
            log.error("Failed to generate daily diet plan for user: {} on date: {}", username, date, e);
            throw new RuntimeException("Failed to generate daily diet plan: " + e.getMessage(), e);
        }
    }

    public List<DietPlan> getWeeklyPlans(String username, LocalDate date) {
        try {
            LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            log.info("Getting weekly diet plans for user: {} from {} to {}", username, startOfWeek, endOfWeek);

            List<DietPlan> existingPlans = dietPlanRepository.findWeeklyPlansByUsernameAndDateRange(username, startOfWeek, endOfWeek);

            // Generate missing plans for the week
            for (LocalDate currentDate = startOfWeek; !currentDate.isAfter(endOfWeek); currentDate = currentDate.plusDays(1)) {
                final LocalDate dateToCheck = currentDate;
                boolean planExists = existingPlans.stream()
                        .anyMatch(plan -> plan.getPlanDate().equals(dateToCheck));

                if (!planExists) {
                    try {
                        DietPlan newPlan = generateDailyPlan(username, currentDate);
                        existingPlans.add(newPlan);
                    } catch (Exception e) {
                        log.warn("Failed to generate diet plan for date: {} for user: {}", currentDate, username, e);
                        // Continue with other dates even if one fails
                    }
                }
            }

            List<DietPlan> sortedPlans = existingPlans.stream()
                    .sorted((p1, p2) -> p1.getPlanDate().compareTo(p2.getPlanDate()))
                    .toList();

            log.info("Retrieved {} diet plans for weekly view for user: {}", sortedPlans.size(), username);
            return sortedPlans;

        } catch (Exception e) {
            log.error("Failed to get weekly diet plans for user: {}", username, e);
            throw new RuntimeException("Failed to get weekly diet plans: " + e.getMessage(), e);
        }
    }

    public DietPlan getDailyPlan(String username, LocalDate date) {
        try {
            log.info("Getting daily diet plan for user: {} on date: {}", username, date);

            return dietPlanRepository.findByUser_UsernameAndPlanDate(username, date)
                    .orElseGet(() -> {
                        log.info("No existing plan found, generating new plan for user: {} on date: {}", username, date);
                        return generateDailyPlan(username, date);
                    });

        } catch (Exception e) {
            log.error("Failed to get daily diet plan for user: {} on date: {}", username, date, e);
            throw new RuntimeException("Failed to get daily diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a single meal for a specific meal type
     */
    public Meal generateSingleMeal(String username, MealType mealType, BigDecimal targetCalories) {
        try {
            log.info("Generating single {} meal for user: {}", mealType, username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = userProfileRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("User profile not found"));
            }

            return aiRecommendationService.generateSingleMeal(profile, mealType, targetCalories);

        } catch (Exception e) {
            log.error("Failed to generate single meal for user: {}", username, e);
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
            Meal newMeal = generateSingleMeal(dietPlan.getUser().getUsername(), mealType, mealCalories);

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
    public DietPlan getCurrentDietPlan(String username) {
        return getDailyPlan(username, LocalDate.now());
    }

    /**
     * Check if user has a diet plan for a specific date
     */
    public boolean hasDietPlan(String username, LocalDate date) {
        return dietPlanRepository.findByUser_UsernameAndPlanDate(username, date).isPresent();
    }

    /**
     * Delete a diet plan for a specific date
     */
    public void deleteDietPlan(String username, LocalDate date) {
        try {
            Optional<DietPlan> dietPlan = dietPlanRepository.findByUser_UsernameAndPlanDate(username, date);
            if (dietPlan.isPresent()) {
                dietPlanRepository.delete(dietPlan.get());
                log.info("Deleted diet plan for user: {} on date: {}", username, date);
            } else {
                log.warn("No diet plan found to delete for user: {} on date: {}", username, date);
            }
        } catch (Exception e) {
            log.error("Failed to delete diet plan for user: {} on date: {}", username, date, e);
            throw new RuntimeException("Failed to delete diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Get all diet plans for a user within a date range
     */
    public List<DietPlan> getDietPlansByDateRange(String username, LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Getting diet plans for user: {} from {} to {}", username, startDate, endDate);
            return dietPlanRepository.findWeeklyPlansByUsernameAndDateRange(username, startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to get diet plans by date range for user: {}", username, e);
            throw new RuntimeException("Failed to get diet plans by date range: " + e.getMessage(), e);
        }
    }
}