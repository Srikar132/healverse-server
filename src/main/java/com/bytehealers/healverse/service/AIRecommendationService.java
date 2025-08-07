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
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class AIRecommendationService {

        private final ChatClient chatClient;
        private final NutritionCalculatorService nutritionCalculator;
        private final ObjectMapper objectMapper;

        @Value("classpath:/prompts/diet-plan-template.txt")
        private Resource dietPlanPromptTemplate;

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
         * Generates individual meals using AI with prompt template
         */
        private List<Meal> generateMeals(UserProfile profile, BigDecimal targetCalories,
                                         NutritionCalculatorService.MacroDistribution macros) {
            try {
                // Build prompt using template
                String promptText = buildDietPlanPrompt(profile, targetCalories, macros);

                // Call AI service
                String response = chatClient.prompt()
                        .user(promptText)
                        .call()
                        .content();

                // Parse and convert response to meals
                return parseMealResponse(response);

            } catch (Exception e) {
                log.error("Failed to generate meals using AI", e);
                throw new RuntimeException("AI meal generation failed", e);
            }
        }

        /**
         * Builds the diet plan prompt using template and user data
         */
        private String buildDietPlanPrompt(UserProfile profile, BigDecimal targetCalories,
                                           NutritionCalculatorService.MacroDistribution macros) {
            try {
                // Create template variables
                Map<String, Object> templateVars = createTemplateVariables(profile, targetCalories, macros);

                // Load and process template
                PromptTemplate promptTemplate = new PromptTemplate(dietPlanPromptTemplate);
                return promptTemplate.render(templateVars);

            } catch (Exception e) {
                log.error("Failed to build diet plan prompt", e);
                // Fallback to hardcoded prompt if template fails
                return buildFallbackPrompt(profile, targetCalories, macros);
            }
        }

        /**
         * Creates template variables for prompt generation
         */
        private Map<String, Object> createTemplateVariables(UserProfile profile,
                                                            BigDecimal targetCalories,
                                                            NutritionCalculatorService.MacroDistribution macros) {
            Map<String, Object> vars = new HashMap<>();

            // User profile data
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

            // Nutritional requirements
            vars.put("targetCalories", targetCalories);
            vars.put("protein", macros.getProtein());
            vars.put("carbs", macros.getCarbs());
            vars.put("fat", macros.getFat());

            // Additional context
            vars.put("mealTypes", List.of("BREAKFAST", "LUNCH", "DINNER"));
            vars.put("currentDate", LocalDate.now().toString());

            return vars;
        }

        /**
         * Fallback prompt builder if template fails
         */
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

        /**
         * Parses AI response and converts to Meal objects
         */
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

        /**
         * Extracts JSON from potentially markdown-formatted response
         */
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

        /**
         * Converts JSON node to Meal object
         */
        private Meal convertJsonToMeal(JsonNode mealNode) {
            Meal meal = new Meal();

            // Required fields
            meal.setMealType(MealType.valueOf(mealNode.get("mealType").asText()));
            meal.setMealName(mealNode.get("mealName").asText());
            meal.setCalories(BigDecimal.valueOf(mealNode.get("calories").asDouble()));
            meal.setProtein(BigDecimal.valueOf(mealNode.get("protein").asDouble()));
            meal.setCarbs(BigDecimal.valueOf(mealNode.get("carbs").asDouble()));
            meal.setFat(BigDecimal.valueOf(mealNode.get("fat").asDouble()));

            // Optional fields with defaults
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

            // Handle ingredients
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

        /**
         * Validates generated meals for completeness and accuracy
         */
        private void validateMeals(List<Meal> meals) {
            if (meals.isEmpty()) {
                throw new IllegalArgumentException("No meals generated");
            }

            // Check for required meal types
            boolean hasBreakfast = meals.stream().anyMatch(m -> m.getMealType() == MealType.BREAKFAST);
            boolean hasLunch = meals.stream().anyMatch(m -> m.getMealType() == MealType.LUNCH);
            boolean hasDinner = meals.stream().anyMatch(m -> m.getMealType() == MealType.DINNER);

            if (!hasBreakfast || !hasLunch || !hasDinner) {
                log.warn("Generated meals missing required meal types. Breakfast: {}, Lunch: {}, Dinner: {}",
                        hasBreakfast, hasLunch, hasDinner);
            }

            // Validate nutritional values
            for (Meal meal : meals) {
                if (meal.getCalories().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invalid calories for meal: " + meal.getMealName());
                }
            }
        }

        /**
         * Creates a complete DietPlan with calculated totals
         */
        private DietPlan createDietPlan(User user, List<Meal> meals, BigDecimal targetCalories,
                                        NutritionCalculatorService.MacroDistribution macros) {
            DietPlan dietPlan = new DietPlan();
            dietPlan.setUser(user);
            dietPlan.setPlanDate(LocalDate.now());
            dietPlan.setIsGenerated(true);
            dietPlan.setCreatedAt(LocalDateTime.now());

            // Set meals and calculate totals
            meals.forEach(meal -> meal.setDietPlan(dietPlan));
            dietPlan.setMeals(meals);

            // Calculate actual totals from meals
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

        /**
         * Generates a meal plan for a specific meal type
         */
        public Meal generateSingleMeal(UserProfile profile, MealType mealType,
                                       BigDecimal targetCalories) {
            try {
                log.info("Generating single {} meal for user profile", mealType);

                NutritionCalculatorService.MacroDistribution macros =
                        nutritionCalculator.calculateMacros(targetCalories, profile.getGoal());

                // Adjust calories for single meal (rough estimates)
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
    }