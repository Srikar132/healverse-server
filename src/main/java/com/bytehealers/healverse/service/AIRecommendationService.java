package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final ChatClient chatClient;
    private final NutritionCalculatorService nutritionCalculator;
    private final ObjectMapper objectMapper;

    @Value("classpath:/prompts/diet-plan-template.txt")
    private Resource dietPlanPromptTemplate;

    @Value("classpath:/prompts/adaptive-diet-plan-template.txt")
    private Resource adaptiveDietPlanPromptTemplate;

    /**
     * Generates a complete diet plan for the user
     */
    public DietPlan generateDietPlan(User user) {
        try {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                throw new IllegalArgumentException("User profile is required to generate diet plan");
            }

            log.info("Generating diet plan for user: {}", user.getUsername());

            // Calculate nutritional requirements
            BigDecimal targetCalories = nutritionCalculator.calculateTargetCalories(profile);

            NutritionCalculatorService.MacroDistribution macros =
                    nutritionCalculator.calculateMacros(targetCalories, profile.getGoal());

            // Generate meals using AI
            List<Meal> meals = generateMeals(profile, targetCalories, macros);

            // Create and populate diet plan
            DietPlan dietPlan = createDietPlan(user, meals, targetCalories, macros);

            log.info("Successfully generated diet plan with {} meals for user: {}",
                    meals.size(), user.getUsername());

            return dietPlan;

        } catch (Exception e) {
            log.error("Failed to generate diet plan for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to generate diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Generates an adaptive diet plan based on historical nutrition data
     */
    public DietPlan generateDietPlanWithHistory(User user, List<DailyNutritionSummary> historicalData,
                                                DailyNutritionSummary todaysSummary) {
        try {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                throw new IllegalArgumentException("User profile is required to generate diet plan");
            }

            log.info("Generating adaptive diet plan for user: {} with {} days of historical data",
                    user.getUsername(), historicalData.size());

            // Calculate base nutritional requirements
            BigDecimal baseTargetCalories = nutritionCalculator.calculateTargetCalories(profile);
            NutritionCalculatorService.MacroDistribution baseMacros =
                    nutritionCalculator.calculateMacros(baseTargetCalories, profile.getGoal());

            // Analyze historical patterns
            NutritionAnalysis analysis = analyzeHistoricalData(historicalData, todaysSummary,
                    baseTargetCalories, baseMacros);

            // Adjust targets based on analysis
            CalorieAdjustment adjustment = calculateCalorieAdjustment(analysis, profile);

            // Generate adaptive meals
            List<Meal> meals = generateAdaptiveMeals(profile, adjustment, baseMacros, analysis);

            // Create diet plan with adjusted targets
            DietPlan dietPlan = createAdaptiveDietPlan(user, meals, adjustment, analysis);

            log.info("Successfully generated adaptive diet plan for user: {} with {} calorie adjustment",
                    user.getUsername(), adjustment.getAdjustedTargetCalories().subtract(baseTargetCalories));

            return dietPlan;

        } catch (Exception e) {
            log.error("Failed to generate adaptive diet plan for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to generate adaptive diet plan: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes historical nutrition data to identify patterns and trends
     */
    private NutritionAnalysis analyzeHistoricalData(List<DailyNutritionSummary> historicalData,
                                                    DailyNutritionSummary todaysSummary,
                                                    BigDecimal baseTargetCalories,
                                                    NutritionCalculatorService.MacroDistribution baseMacros) {

        NutritionAnalysis analysis = new NutritionAnalysis();

        if (historicalData.isEmpty()) {
            return analysis; // Return empty analysis if no historical data
        }

        // Calculate averages from historical data
        BigDecimal avgConsumedCalories = historicalData.stream()
                .map(DailyNutritionSummary::getConsumedCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historicalData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgConsumedProtein = historicalData.stream()
                .map(DailyNutritionSummary::getConsumedProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historicalData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgConsumedCarbs = historicalData.stream()
                .map(DailyNutritionSummary::getConsumedCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historicalData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgConsumedFat = historicalData.stream()
                .map(DailyNutritionSummary::getConsumedFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historicalData.size()), 2, RoundingMode.HALF_UP);

        // Calculate adherence rates
        BigDecimal avgTargetCalories = historicalData.stream()
                .map(DailyNutritionSummary::getTargetCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historicalData.size()), 2, RoundingMode.HALF_UP);

        analysis.setAvgConsumedCalories(avgConsumedCalories);
        analysis.setAvgConsumedProtein(avgConsumedProtein);
        analysis.setAvgConsumedCarbs(avgConsumedCarbs);
        analysis.setAvgConsumedFat(avgConsumedFat);

        // Calculate adherence percentage
        if (avgTargetCalories.compareTo(BigDecimal.ZERO) > 0) {
            analysis.setCalorieAdherence(avgConsumedCalories.divide(avgTargetCalories, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        // Identify consumption patterns
        analysis.setConsistentOvereating(
                historicalData.stream()
                        .mapToLong(day -> day.getConsumedCalories().compareTo(day.getTargetCalories()) > 0 ? 1L : 0L)
                        .sum() >= historicalData.size() * 0.7 // 70% of days
        );

        analysis.setConsistentUndereating(
                historicalData.stream()
                        .mapToLong(day -> day.getConsumedCalories().compareTo(day.getTargetCalories()) < 0 ? 1L : 0L)
                        .sum() >= historicalData.size() * 0.7 // 70% of days
        );

        // Calculate today's consumption if available
        if (todaysSummary != null) {
            analysis.setTodayConsumedCalories(todaysSummary.getConsumedCalories());
            analysis.setTodayRemainingCalories(todaysSummary.getRemainingCalories());
            analysis.setTodayTargetCalories(todaysSummary.getTargetCalories());
        }

        return analysis;
    }

    /**
     * Calculates calorie adjustments based on historical analysis
     */
    private CalorieAdjustment calculateCalorieAdjustment(NutritionAnalysis analysis, UserProfile profile) {
        CalorieAdjustment adjustment = new CalorieAdjustment();

        BigDecimal baseTarget = nutritionCalculator.calculateTargetCalories(profile);
        BigDecimal adjustedTarget = baseTarget;
        String adjustmentReason = "Standard calorie target";

        // If user has been consistently overeating, reduce today's target
        if (analysis.isConsistentOvereating()) {
            BigDecimal overage = analysis.getAvgConsumedCalories().subtract(baseTarget);
            if (overage.compareTo(BigDecimal.valueOf(200)) > 0) { // Significant overage
                adjustedTarget = baseTarget.subtract(BigDecimal.valueOf(150)); // Reduce by 150 calories
                adjustmentReason = "Reducing calories due to consistent overeating pattern";
            }
        }

        // If user has been consistently undereating, increase today's target slightly
        else if (analysis.isConsistentUndereating()) {
            BigDecimal shortage = baseTarget.subtract(analysis.getAvgConsumedCalories());
            if (shortage.compareTo(BigDecimal.valueOf(200)) > 0) { // Significant shortage
                adjustedTarget = baseTarget.add(BigDecimal.valueOf(100)); // Add 100 calories
                adjustmentReason = "Increasing calories due to consistent undereating pattern";
            }
        }

        // If user has already consumed calories today, adjust remaining target
        if (analysis.getTodayConsumedCalories() != null &&
                analysis.getTodayConsumedCalories().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal remainingCalories = adjustedTarget.subtract(analysis.getTodayConsumedCalories());

            if (remainingCalories.compareTo(BigDecimal.valueOf(200)) < 0) {
                // Very few calories remaining - focus on nutrient-dense, low-calorie options
                adjustedTarget = analysis.getTodayConsumedCalories().add(BigDecimal.valueOf(300));
                adjustmentReason = "Low-calorie plan due to calories already consumed today";
            } else if (remainingCalories.compareTo(adjustedTarget.multiply(BigDecimal.valueOf(0.3))) < 0) {
                // Less than 30% of target remaining - moderate adjustment
                adjustmentReason = "Modified plan based on today's consumption";
            }
        }

        adjustment.setBaseTargetCalories(baseTarget);
        adjustment.setAdjustedTargetCalories(adjustedTarget);
        adjustment.setAdjustmentReason(adjustmentReason);

        return adjustment;
    }

    /**
     * Generates meals using adaptive AI prompting based on historical analysis
     */
    private List<Meal> generateAdaptiveMeals(UserProfile profile, CalorieAdjustment adjustment,
                                             NutritionCalculatorService.MacroDistribution baseMacros,
                                             NutritionAnalysis analysis) {
        try {
            String promptText = buildAdaptiveDietPlanPrompt(profile, adjustment, baseMacros, analysis);

            String response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            return parseMealResponse(response);

        } catch (Exception e) {
            log.error("Failed to generate adaptive meals using AI", e);
            throw new RuntimeException("Adaptive meal generation failed", e);
        }
    }

    /**
     * Builds adaptive diet plan prompt with historical context
     */
    private String buildAdaptiveDietPlanPrompt(UserProfile profile, CalorieAdjustment adjustment,
                                               NutritionCalculatorService.MacroDistribution baseMacros,
                                               NutritionAnalysis analysis) {

        Map<String, Object> templateVars = createTemplateVariables(profile, adjustment.getAdjustedTargetCalories(), baseMacros);

        // Add historical context
        templateVars.put("adjustmentReason", adjustment.getAdjustmentReason());
        templateVars.put("baseTargetCalories", adjustment.getBaseTargetCalories());
        templateVars.put("adjustedTargetCalories", adjustment.getAdjustedTargetCalories());
        templateVars.put("avgConsumedCalories", analysis.getAvgConsumedCalories() != null ? analysis.getAvgConsumedCalories() : BigDecimal.ZERO);
        templateVars.put("calorieAdherence", analysis.getCalorieAdherence() != null ? analysis.getCalorieAdherence() : BigDecimal.ZERO);
        templateVars.put("consistentOvereating", analysis.isConsistentOvereating());
        templateVars.put("consistentUndereating", analysis.isConsistentUndereating());
        templateVars.put("todayConsumedCalories", analysis.getTodayConsumedCalories() != null ? analysis.getTodayConsumedCalories() : BigDecimal.ZERO);

        try {
            if (adaptiveDietPlanPromptTemplate != null && adaptiveDietPlanPromptTemplate.exists()) {
                PromptTemplate promptTemplate = new PromptTemplate(adaptiveDietPlanPromptTemplate);
                return promptTemplate.render(templateVars);
            }
        } catch (Exception e) {
            log.error("Failed to load adaptive template, using fallback", e);
        }

        return buildAdaptiveFallbackPrompt(profile, adjustment, baseMacros, analysis);
    }

    /**
     * Fallback adaptive prompt builder
     */
    private String buildAdaptiveFallbackPrompt(UserProfile profile, CalorieAdjustment adjustment,
                                               NutritionCalculatorService.MacroDistribution baseMacros,
                                               NutritionAnalysis analysis) {
        return String.format("""
            Generate a personalized ADAPTIVE Indian diet plan in JSON format based on user's eating history:
            
            User Profile: %s, Age: %d, Height: %s cm, Weight: %s kg → %s kg
            Goal: %s, Activity: %s, Diet: %s, Health: %s
            
            HISTORICAL ANALYSIS:
            - Base Target: %s kcal → Adjusted Target: %s kcal
            - Reason: %s
            - Average Consumption: %s kcal (Adherence: %s%%)
            - Today Already Consumed: %s kcal
            - Consistent Overeating: %s | Undereating: %s
            
            ADJUSTED NUTRITION TARGETS:
            Target: %s kcal, Protein: %sg, Carbs: %sg, Fat: %sg
            
            Provide exactly 3 meals (BREAKFAST, LUNCH, DINNER) considering the user's eating patterns:
            
            {
              "meals": [
                {
                  "mealType": "BREAKFAST",
                  "mealName": "Recipe Name",
                  "calories": 350,
                  "protein": 15,
                  "carbs": 45,
                  "fat": 12,
                  "preparationTimeMinutes": 15,
                  "instructions": "Detailed cooking steps",
                  "healthBenefits": "Health benefits addressing user's eating patterns",
                  "ingredients": ["ingredient1 with quantity", "ingredient2 with quantity"]
                }
              ]
            }
            
            ADAPTIVE REQUIREMENTS:
            1. %s
            2. Focus on %s foods if overeating pattern detected
            3. Include %s options if undereating pattern detected  
            4. Consider today's consumption: %s kcal already eaten
            5. Provide practical portion control tips in instructions
            6. Address dietary restriction: %s
            7. Support health condition: %s
            8. Make recipes appealing to improve adherence
            """,
                profile.getGender(), profile.getAge(), profile.getHeightCm(),
                profile.getCurrentWeightKg(), profile.getTargetWeightKg(),
                profile.getGoal(), profile.getActivityLevel(), profile.getDietaryRestriction(),
                profile.getHealthCondition(),
                adjustment.getBaseTargetCalories(), adjustment.getAdjustedTargetCalories(),
                adjustment.getAdjustmentReason(),
                analysis.getAvgConsumedCalories(), analysis.getCalorieAdherence(),
                analysis.getTodayConsumedCalories(), analysis.isConsistentOvereating(), analysis.isConsistentUndereating(),
                adjustment.getAdjustedTargetCalories(), baseMacros.getProtein(), baseMacros.getCarbs(), baseMacros.getFat(),
                adjustment.getAdjustmentReason(),
                analysis.isConsistentOvereating() ? "high-fiber, low-calorie" : "nutrient-dense",
                analysis.isConsistentUndereating() ? "calorie-dense, appealing" : "balanced",
                analysis.getTodayConsumedCalories(),
                profile.getDietaryRestriction(), profile.getHealthCondition());
    }

    /**
     * Creates an adaptive diet plan with adjusted targets
     */
    private DietPlan createAdaptiveDietPlan(User user, List<Meal> meals, CalorieAdjustment adjustment,
                                            NutritionAnalysis analysis) {
        DietPlan dietPlan = new DietPlan();
        dietPlan.setUser(user);
        dietPlan.setPlanDate(LocalDate.now());
        dietPlan.setIsGenerated(true);
        dietPlan.setCreatedAt(LocalDateTime.now());

        // Set meals with relationship
        meals.forEach(meal -> meal.setDietPlan(dietPlan));
        dietPlan.setMeals(meals);

        // Calculate totals from generated meals
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

        return dietPlan;
    }

    // === EXISTING METHODS (unchanged) ===

    private List<Meal> generateMeals(UserProfile profile, BigDecimal targetCalories,
                                     NutritionCalculatorService.MacroDistribution macros) {
        try {
            String promptText = buildDietPlanPrompt(profile, targetCalories, macros);

            String response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            return parseMealResponse(response);

        } catch (Exception e) {
            log.error("Failed to generate meals using AI", e);
            throw new RuntimeException("AI meal generation failed", e);
        }
    }

    private String buildDietPlanPrompt(UserProfile profile, BigDecimal targetCalories,
                                       NutritionCalculatorService.MacroDistribution macros) {
        try {
            Map<String, Object> templateVars = createTemplateVariables(profile, targetCalories, macros);
            PromptTemplate promptTemplate = new PromptTemplate(dietPlanPromptTemplate);
            return promptTemplate.render(templateVars);

        } catch (Exception e) {
            log.error("Failed to build diet plan prompt", e);
            return buildFallbackPrompt(profile, targetCalories, macros);
        }
    }

    private Map<String, Object> createTemplateVariables(UserProfile profile,
                                                        BigDecimal targetCalories,
                                                        NutritionCalculatorService.MacroDistribution macros) {
        Map<String, Object> vars = new HashMap<>();

        vars.put("gender", profile.getGender().toString());
        vars.put("age", profile.getAge());
        vars.put("height", profile.getHeightCm());
        vars.put("currentWeight", profile.getCurrentWeightKg());
        vars.put("targetWeight", profile.getTargetWeightKg());
        vars.put("goal", profile.getGoal().toString());
        vars.put("activityLevel", profile.getActivityLevel().toString());
        vars.put("dietaryRestriction", profile.getDietaryRestriction().toString());
        vars.put("healthCondition", profile.getHealthCondition().toString());
        vars.put("weightLossSpeed", profile.getWeightLossSpeed().toString());
        vars.put("address" , profile.getAddress());

        vars.put("targetCalories", targetCalories);
        vars.put("protein", macros.getProtein());
        vars.put("carbs", macros.getCarbs());
        vars.put("fat", macros.getFat());

        vars.put("mealTypes", List.of("BREAKFAST", "LUNCH", "DINNER"));
        vars.put("currentDate", LocalDate.now().toString());

        return vars;
    }

    private String buildFallbackPrompt(UserProfile profile, BigDecimal targetCalories,
                                       NutritionCalculatorService.MacroDistribution macros) {
        return String.format("""
            Generate a personalized Indian diet plan in JSON format:
            
            User: %s, Age: %d, Height: %s cm, Weight: %s kg → %s kg
            Goal: %s, Activity: %s, Diet: %s, Health: %s
            
            Nutrition Target: %s kcal, Protein: %sg, Carbs: %sg, Fat: %sg
            
            Provide exactly 3 meals (BREAKFAST, LUNCH, DINNER) in this JSON structure:
            {
              "meals": [
                {
                  "mealType": "BREAKFAST",
                  "mealName": "Recipe Name",
                  "calories": 350,
                  "protein": 15,
                  "carbs": 45,
                  "fat": 12,
                  "preparationTimeMinutes": 15,
                  "instructions": "Detailed cooking steps",
                  "healthBenefits": "Health benefits",
                  "ingredients": ["ingredient1", "ingredient2"]
                }
              ]
            }
            
            Requirements:
            - Use traditional Indian recipes
            - Consider dietary restrictions: %s
            - Address health condition: %s
            - Ensure nutritional balance
            - Provide practical home cooking instructions
            """,
                profile.getGender(), profile.getAge(), profile.getHeightCm(),
                profile.getCurrentWeightKg(), profile.getTargetWeightKg(),
                profile.getGoal(), profile.getActivityLevel(), profile.getDietaryRestriction(),
                profile.getHealthCondition(), targetCalories, macros.getProtein(),
                macros.getCarbs(), macros.getFat(), profile.getDietaryRestriction(),
                profile.getHealthCondition());
    }

    private List<Meal> parseMealResponse(String response) throws Exception {
        String jsonResponse = extractJsonFromResponse(response);

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode mealsNode = rootNode.get("meals");

        if (mealsNode == null || !mealsNode.isArray()) {
            throw new IllegalArgumentException("Invalid AI response format: missing meals array");
        }

        List<Meal> meals = new ArrayList<>();
        for (JsonNode mealNode : mealsNode) {
            meals.add(convertJsonToMeal(mealNode));
        }

        validateMeals(meals);
        return meals;
    }

    private String extractJsonFromResponse(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }

        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }

        return response.trim();
    }

    private Meal convertJsonToMeal(JsonNode mealNode) {
        Meal meal = new Meal();

        meal.setMealType(MealType.valueOf(mealNode.get("mealType").asText()));
        meal.setMealName(mealNode.get("mealName").asText());
        meal.setCalories(BigDecimal.valueOf(mealNode.get("calories").asDouble()));
        meal.setProtein(BigDecimal.valueOf(mealNode.get("protein").asDouble()));
        meal.setCarbs(BigDecimal.valueOf(mealNode.get("carbs").asDouble()));
        meal.setFat(BigDecimal.valueOf(mealNode.get("fat").asDouble()));

        meal.setPreparationTimeMinutes(
                mealNode.has("preparationTimeMinutes") ?
                        mealNode.get("preparationTimeMinutes").asInt() : 30
        );

        meal.setInstructions(
                mealNode.has("instructions") ?
                        mealNode.get("instructions").asText() : "Instructions not provided"
        );

        meal.setHealthBenefits(
                mealNode.has("healthBenefits") ?
                        mealNode.get("healthBenefits").asText() : "Nutritious and balanced meal"
        );

        if (mealNode.has("ingredients")) {
            List<String> ingredients = new ArrayList<>();
            JsonNode ingredientsNode = mealNode.get("ingredients");

            if (ingredientsNode.isArray()) {
                for (JsonNode ingredient : ingredientsNode) {
                    ingredients.add(ingredient.asText());
                }
            }
            meal.setIngredients(ingredients);
        }

        return meal;
    }

    private void validateMeals(List<Meal> meals) {
        if (meals.isEmpty()) {
            throw new IllegalArgumentException("No meals generated");
        }

        boolean hasBreakfast = meals.stream().anyMatch(m -> m.getMealType() == MealType.BREAKFAST);
        boolean hasLunch = meals.stream().anyMatch(m -> m.getMealType() == MealType.LUNCH);
        boolean hasDinner = meals.stream().anyMatch(m -> m.getMealType() == MealType.DINNER);

        if (!hasBreakfast || !hasLunch || !hasDinner) {
            log.warn("Generated meals missing required meal types. Breakfast: {}, Lunch: {}, Dinner: {}",
                    hasBreakfast, hasLunch, hasDinner);
        }

        for (Meal meal : meals) {
            if (meal.getCalories().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid calories for meal: " + meal.getMealName());
            }
        }
    }

    private DietPlan createDietPlan(User user, List<Meal> meals, BigDecimal targetCalories,
                                    NutritionCalculatorService.MacroDistribution macros) {
        DietPlan dietPlan = new DietPlan();
        dietPlan.setUser(user);
        dietPlan.setPlanDate(LocalDate.now());
        dietPlan.setIsGenerated(true);
        dietPlan.setCreatedAt(LocalDateTime.now());

        meals.forEach(meal -> meal.setDietPlan(dietPlan));
        dietPlan.setMeals(meals);

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

        return dietPlan;
    }

    public Meal generateSingleMeal(UserProfile profile, MealType mealType, BigDecimal targetCalories) {
        try {
            log.info("Generating single {} meal for user profile", mealType);

            NutritionCalculatorService.MacroDistribution macros =
                    nutritionCalculator.calculateMacros(targetCalories, profile.getGoal());

            BigDecimal mealCalories = switch (mealType) {
                case BREAKFAST -> targetCalories.multiply(BigDecimal.valueOf(0.25));
                case LUNCH -> targetCalories.multiply(BigDecimal.valueOf(0.40));
                case DINNER -> targetCalories.multiply(BigDecimal.valueOf(0.35));
                case SNACK -> targetCalories.multiply(BigDecimal.valueOf(0.10));
            };

            String prompt = buildSingleMealPrompt(profile, mealType, mealCalories, macros);
            String response = chatClient.prompt().user(prompt).call().content();

            List<Meal> meals = parseMealResponse(response);
            return meals.isEmpty() ? null : meals.get(0);

        } catch (Exception e) {
            log.error("Failed to generate single meal", e);
            throw new RuntimeException("Single meal generation failed", e);
        }
    }

    private String buildSingleMealPrompt(UserProfile profile, MealType mealType,
                                         BigDecimal mealCalories,
                                         NutritionCalculatorService.MacroDistribution macros) {
        return String.format("""
            Generate a single Indian %s recipe in JSON format:
            
            User Requirements:
            - Dietary Restriction: %s
            - Health Condition: %s
            - Target Calories for this meal: %s kcal
            
            Provide exactly 1 meal in this JSON structure:
            {
              "meals": [
                {
                  "mealType": "%s",
                  "mealName": "Recipe Name",
                  "calories": %s,
                  "protein": 15,
                  "carbs": 45,
                  "fat": 12,
                  "preparationTimeMinutes": 15,
                  "instructions": "Step by step cooking instructions",
                  "healthBenefits": "Health benefits of this meal",
                  "ingredients": ["ingredient1", "ingredient2", "ingredient3"]
                }
              ]
            }
            
            Make it authentic Indian cuisine suitable for %s dietary preference.
            """,
                mealType, profile.getDietaryRestriction(), profile.getHealthCondition(),
                mealCalories, mealType, mealCalories.intValue(), profile.getDietaryRestriction());
    }

    // === HELPER CLASSES FOR HISTORICAL ANALYSIS ===

    /**
     * Analysis results from historical nutrition data
     */
    public static class NutritionAnalysis {
        private BigDecimal avgConsumedCalories = BigDecimal.ZERO;
        private BigDecimal avgConsumedProtein = BigDecimal.ZERO;
        private BigDecimal avgConsumedCarbs = BigDecimal.ZERO;
        private BigDecimal avgConsumedFat = BigDecimal.ZERO;
        private BigDecimal calorieAdherence = BigDecimal.ZERO;
        private boolean consistentOvereating = false;
        private boolean consistentUndereating = false;
        private BigDecimal todayConsumedCalories = BigDecimal.ZERO;
        private BigDecimal todayRemainingCalories = BigDecimal.ZERO;
        private BigDecimal todayTargetCalories = BigDecimal.ZERO;

        // Getters and Setters
        public BigDecimal getAvgConsumedCalories() { return avgConsumedCalories; }
        public void setAvgConsumedCalories(BigDecimal avgConsumedCalories) { this.avgConsumedCalories = avgConsumedCalories; }

        public BigDecimal getAvgConsumedProtein() { return avgConsumedProtein; }
        public void setAvgConsumedProtein(BigDecimal avgConsumedProtein) { this.avgConsumedProtein = avgConsumedProtein; }

        public BigDecimal getAvgConsumedCarbs() { return avgConsumedCarbs; }
        public void setAvgConsumedCarbs(BigDecimal avgConsumedCarbs) { this.avgConsumedCarbs = avgConsumedCarbs; }

        public BigDecimal getAvgConsumedFat() { return avgConsumedFat; }
        public void setAvgConsumedFat(BigDecimal avgConsumedFat) { this.avgConsumedFat = avgConsumedFat; }

        public BigDecimal getCalorieAdherence() { return calorieAdherence; }
        public void setCalorieAdherence(BigDecimal calorieAdherence) { this.calorieAdherence = calorieAdherence; }

        public boolean isConsistentOvereating() { return consistentOvereating; }
        public void setConsistentOvereating(boolean consistentOvereating) { this.consistentOvereating = consistentOvereating; }

        public boolean isConsistentUndereating() { return consistentUndereating; }
        public void setConsistentUndereating(boolean consistentUndereating) { this.consistentUndereating = consistentUndereating; }

        public BigDecimal getTodayConsumedCalories() { return todayConsumedCalories; }
        public void setTodayConsumedCalories(BigDecimal todayConsumedCalories) { this.todayConsumedCalories = todayConsumedCalories; }

        public BigDecimal getTodayRemainingCalories() { return todayRemainingCalories; }
        public void setTodayRemainingCalories(BigDecimal todayRemainingCalories) { this.todayRemainingCalories = todayRemainingCalories; }

        public BigDecimal getTodayTargetCalories() { return todayTargetCalories; }
        public void setTodayTargetCalories(BigDecimal todayTargetCalories) { this.todayTargetCalories = todayTargetCalories; }
    }

    /**
     * Calorie adjustment calculations
     */
    public static class CalorieAdjustment {
        private BigDecimal baseTargetCalories = BigDecimal.ZERO;
        private BigDecimal adjustedTargetCalories = BigDecimal.ZERO;
        private String adjustmentReason = "";

        public BigDecimal getBaseTargetCalories() { return baseTargetCalories; }
        public void setBaseTargetCalories(BigDecimal baseTargetCalories) { this.baseTargetCalories = baseTargetCalories; }

        public BigDecimal getAdjustedTargetCalories() { return adjustedTargetCalories; }
        public void setAdjustedTargetCalories(BigDecimal adjustedTargetCalories) { this.adjustedTargetCalories = adjustedTargetCalories; }

        public String getAdjustmentReason() { return adjustmentReason; }
        public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }
    }
}