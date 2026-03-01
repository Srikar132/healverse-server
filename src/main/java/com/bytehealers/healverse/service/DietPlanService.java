package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

//    public DietPlan generateDailyPlanWithHistory(Long userId, LocalDate date) {
//        try {
//            // Check if plan already exists
//            Optional<DietPlan> existingPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
//            if (existingPlan.isPresent()) {
//                log.info("Returning existing diet plan for user: {} on date: {}", userId, date);
//                return existingPlan.get();
//            }
//
//            // Get user with profile
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            UserProfile profile = user.getProfile();
//            if (profile == null) {
//                profile = userProfileRepository.findByUser(user)
//                        .orElseThrow(() -> new RuntimeException("User profile not found"));
//                user.setProfile(profile);
//            }
//
//            log.info("Generating daily diet plan with historical data for user: {} on date: {}", userId, date);
//
//            // Get historical nutrition data (last 7 days)
//            LocalDate startDate = date.minusDays(7);
//            List<DailyNutritionSummary> historicalData = dailyNutritionSummaryRepository
//                    .findByUserIdAndDateBetween(userId, startDate, date.minusDays(1));
//
//            // Get yesterday's data specifically
//            Optional<DailyNutritionSummary> yesterdayData = dailyNutritionSummaryRepository
//                    .findByUserIdAndDate(userId, date.minusDays(1));
//
//            // Generate enhanced diet plan using AI service with historical context
//            DietPlan aiGeneratedPlan = aiRecommendationService.generateDietPlanWithHistory(
//                    user, historicalData, yesterdayData.orElse(null));
//
//            // Update the plan with the specific date
//            aiGeneratedPlan.setPlanDate(date);
//            aiGeneratedPlan.setUser(user);
//
//            // Save the diet plan
//            DietPlan savedPlan = dietPlanRepository.save(aiGeneratedPlan);
//
//            // Save meals with proper diet plan reference
//            if (aiGeneratedPlan.getMeals() != null) {
//                for (Meal meal : aiGeneratedPlan.getMeals()) {
//                    meal.setDietPlan(savedPlan);
//                    mealRepository.save(meal);
//                }
//            }
//
//            log.info("Successfully generated and saved diet plan with {} meals considering historical data for user: {} on date: {}",
//                    aiGeneratedPlan.getMeals().size(), userId, date);
//
//            return savedPlan;
//
//        } catch (Exception e) {
//            log.error("Failed to generate daily diet plan with history for user: {} on date: {}", userId, date, e);
//            throw new RuntimeException("Failed to generate daily diet plan: " + e.getMessage(), e);
//        }
//    }

    /**
     * Generate nutritional adjustment context based on historical data
     */
//    public String generateNutritionalContext(List<DailyNutritionSummary> historicalData,
//                                             DailyNutritionSummary yesterdayData) {
//        StringBuilder context = new StringBuilder();
//
//        if (yesterdayData != null) {
//            context.append("Yesterday's Performance:\n");
//
//            // Check if user exceeded targets
//            if (yesterdayData.getConsumedCalories().compareTo(yesterdayData.getTargetCalories()) > 0) {
//                BigDecimal excess = yesterdayData.getConsumedCalories().subtract(yesterdayData.getTargetCalories());
//                context.append(String.format("- EXCEEDED calorie target by %.0f kcal (consumed: %.0f, target: %.0f)\n",
//                        excess, yesterdayData.getConsumedCalories(), yesterdayData.getTargetCalories()));
//            }
//
//            if (yesterdayData.getConsumedProtein().compareTo(yesterdayData.getTargetProtein()) > 0) {
//                BigDecimal excess = yesterdayData.getConsumedProtein().subtract(yesterdayData.getTargetProtein());
//                context.append(String.format("- EXCEEDED protein target by %.1f g\n", excess));
//            }
//
//            if (yesterdayData.getConsumedCarbs().compareTo(yesterdayData.getTargetCarbs()) > 0) {
//                BigDecimal excess = yesterdayData.getConsumedCarbs().subtract(yesterdayData.getTargetCarbs());
//                context.append(String.format("- EXCEEDED carbs target by %.1f g\n", excess));
//            }
//
//            if (yesterdayData.getConsumedFat().compareTo(yesterdayData.getTargetFat()) > 0) {
//                BigDecimal excess = yesterdayData.getConsumedFat().subtract(yesterdayData.getTargetFat());
//                context.append(String.format("- EXCEEDED fat target by %.1f g\n", excess));
//            }
//        }
//
//        if (!historicalData.isEmpty()) {
//            context.append("\nWeekly Trends:\n");
//
//            // Calculate weekly averages
//            BigDecimal avgCalorieExcess = historicalData.stream()
//                    .map(data -> data.getConsumedCalories().subtract(data.getTargetCalories()))
//                    .reduce(BigDecimal.ZERO, BigDecimal::add)
//                    .divide(BigDecimal.valueOf(historicalData.size()), 2, BigDecimal.ROUND_HALF_UP);
//
//            if (avgCalorieExcess.compareTo(BigDecimal.ZERO) > 0) {
//                context.append(String.format("- Average daily calorie excess: %.0f kcal\n", avgCalorieExcess));
//            }
//
//            // Count days exceeded targets
//            long daysExceededCalories = historicalData.stream()
//                    .mapToLong(data -> data.getConsumedCalories().compareTo(data.getTargetCalories()) > 0 ? 1 : 0)
//                    .sum();
//
//            context.append(String.format("- Days exceeded calorie target: %d out of %d\n",
//                    daysExceededCalories, historicalData.size()));
//        }
//
//        return context.toString();
//    }

    /**
     * Enhanced prompt that includes historical data context
     */
//    public String buildEnhancedDietPrompt(User user, String nutritionalContext) {
//        UserProfile profile = user.getProfile();
//
//        // Calculate nutritional requirements (you'll need to implement this logic)
//        BigDecimal targetCalories = calculateTargetCalories(profile);
//        BigDecimal protein = calculateProteinNeeds(profile);
//        BigDecimal carbs = calculateCarbNeeds(profile);
//        BigDecimal fat = calculateFatNeeds(profile);
//
//        return String.format("""
//                Generate a personalized Indian diet plan with the following requirements:
//
//                User Profile:
//                - Gender: %s
//                - Age: %d years
//                - Height: %.0f cm
//                - Current Weight: %.1f kg
//                - Target Weight: %.1f kg
//                - Goal: %s
//                - Activity Level: %s
//                - Dietary Restriction: %s
//                - Health Condition: %s
//                - Weight Loss Speed: %s
//
//                Nutritional Requirements:
//                - Daily Calories: %.0f kcal
//                - Protein: %.0f g
//                - Carbohydrates: %.0f g
//                - Fat: %.0f g
//
//                HISTORICAL EATING PATTERN ANALYSIS:
//                %s
//
//                ADJUSTMENT INSTRUCTIONS:
//                Based on the historical data above, please make the following adjustments to today's meal plan:
//                1. If user exceeded calories yesterday, reduce portion sizes by 10-15%% and focus on high-fiber, low-calorie foods
//                2. If user consistently exceeds targets, emphasize more filling, lower-calorie options like vegetables and lean proteins
//                3. If user exceeded carbs, reduce rice/roti portions and increase vegetable portions
//                4. If user exceeded fats, minimize oil usage and avoid fried foods
//                5. Include more satiating foods to prevent overeating
//
//                Please provide exactly 3 meals (BREAKFAST, LUNCH, DINNER) in JSON format with the following structure:
//
//                {
//                  "meals": [
//                    {
//                      "mealType": "BREAKFAST",
//                      "mealName": "Recipe Name",
//                      "calories": 350,
//                      "protein": 15,
//                      "carbs": 45,
//                      "fat": 12,
//                      "preparationTimeMinutes": 15,
//                      "instructions": "Step by step cooking instructions with exact measurements and cooking times",
//                      "healthBenefits": "Specific health benefits considering user's eating patterns and goals",
//                      "ingredients": [
//                        "ingredient 1 with quantity",
//                        "ingredient 2 with quantity",
//                        "ingredient 3 with quantity"
//                      ]
//                    },
//                    {
//                      "mealType": "LUNCH",
//                      "mealName": "Recipe Name",
//                      "calories": 450,
//                      "protein": 25,
//                      "carbs": 55,
//                      "fat": 18,
//                      "preparationTimeMinutes": 25,
//                      "instructions": "Step by step cooking instructions",
//                      "healthBenefits": "Health benefits addressing historical overconsumption patterns",
//                      "ingredients": [
//                        "ingredient 1 with quantity",
//                        "ingredient 2 with quantity"
//                      ]
//                    },
//                    {
//                      "mealType": "DINNER",
//                      "mealName": "Recipe Name",
//                      "calories": 400,
//                      "protein": 20,
//                      "carbs": 40,
//                      "fat": 15,
//                      "preparationTimeMinutes": 30,
//                      "instructions": "Step by step cooking instructions",
//                      "healthBenefits": "Health benefits for evening meal considering daily nutritional balance",
//                      "ingredients": [
//                        "ingredient 1 with quantity",
//                        "ingredient 2 with quantity"
//                      ]
//                    }
//                  ]
//                }
//
//                IMPORTANT REQUIREMENTS:
//                1. Use traditional Indian ingredients and cooking methods
//                2. Strictly consider dietary restrictions: %s
//                3. Address health condition: %s in meal selection
//                4. COMPENSATE for historical overconsumption patterns shown above
//                5. Provide exact ingredient quantities (e.g., "200g basmati rice", "1 cup dal")
//                6. Focus on high-satiety, nutrient-dense foods to prevent overeating
//                7. Total calories should be adjusted based on historical performance
//                8. Include foods that help control cravings and promote satiety
//                9. Emphasize portion control and mindful eating
//                10. Provide specific health benefits relevant to correcting eating patterns
//
//                Focus on creating a corrective meal plan that helps the user get back on track with their nutritional goals.
//                """,
//                profile.getGender(),
//                profile.getAge(),
//                profile.getHeightCm(),
//                profile.getCurrentWeightKg(),
//                profile.getTargetWeightKg(),
//                profile.getGoal(),
//                profile.getActivityLevel(),
//                profile.getDietaryRestriction(),
//                profile.getHealthCondition(),
//                profile.getWeightLossSpeed(),
//                targetCalories,
//                protein,
//                carbs,
//                fat,
//                nutritionalContext,
//                profile.getDietaryRestriction(),
//                profile.getHealthCondition()
//        );
//    }

    // Placeholder methods - implement based on your business logic
//    private BigDecimal calculateTargetCalories(UserProfile profile) {
//        // Implement your calorie calculation logic
//        return new BigDecimal("2000");
//    }
//
//    private BigDecimal calculateProteinNeeds(UserProfile profile) {
//        // Implement your protein calculation logic
//        return new BigDecimal("150");
//    }
//
//    private BigDecimal calculateCarbNeeds(UserProfile profile) {
//        // Implement your carbs calculation logic
//        return new BigDecimal("250");
//    }
//
//    private BigDecimal calculateFatNeeds(UserProfile profile) {
//        // Implement your fat calculation logic
//        return new BigDecimal("65");
//    }

    @Transactional
    public DietPlan generateDailyPlan(Long userId, LocalDate date) {
        try {

            User user = userRepository.findByIdWithLock(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            // Check if plan already exists
            Optional<DietPlan> existingPlan = dietPlanRepository.findByUserIdAndPlanDate(userId, date);
            if (existingPlan.isPresent()) {
                log.info("Returning existing diet plan for user: {} on date: {}", userId, date);
                return existingPlan.get(); // Safe - checked with isPresent()
            }

//             if plan not exits , only generate if date is >= today's datae , other wise return null object
//            if (date.isBefore(LocalDate.now())) {
//                log.warn("Cannot generate diet plan for past date: {} (today is {})", date, LocalDate.now());
//                return null;
//            }

            UserProfile profile = user.getProfile();

            if (profile == null) {
                profile = userProfileRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("User profile not found"));
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

    @Transactional
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
    @Transactional
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
    @Transactional
    public DietPlan regenerateAllMeals(Long dietPlanId) {
        try {
            log.info("Regenerating all meals for diet plan: {}", dietPlanId);

            DietPlan dietPlan = dietPlanRepository.findById(dietPlanId)
                    .orElseThrow(() -> new RuntimeException("Diet plan not found"));

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

        } catch (Exception e) {
            log.error("Failed to regenerate all meals for diet plan: {}", dietPlanId, e);
            throw new RuntimeException("Failed to regenerate all meals: " + e.getMessage(), e);
        }
    }

    // Updated single meal regeneration method
    @Transactional
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

            // Save updated plan (cascade should handle meal saving)
            DietPlan savedPlan = dietPlanRepository.save(dietPlan);

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

    public DietPlan getTodaysDietPlan(Long userId) {
        return getDailyPlan(userId, LocalDate.now());
    }
    

}
