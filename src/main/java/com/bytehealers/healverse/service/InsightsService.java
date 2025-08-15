package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.internal.DailyHealthData;
import com.bytehealers.healverse.dto.internal.InsightItem;
import com.bytehealers.healverse.dto.response.InsightsResponse;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {

    private final ChatClient chatClient;
    private final UserProfileRepository userProfileRepository;
    private final DailyNutritionSummaryRepository dailyNutritionSummaryRepository;
    private final FoodLogRepository foodLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WaterLogRepository waterLogRepository;
    private final MedicationLogRepository medicationLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InsightsResponse generateDailyInsights(Long userId) {
        try {
            // Get yesterday's date
            LocalDate yesterday = LocalDate.now();

            // Collect all health data from yesterday
            DailyHealthData healthData = collectDailyHealthData(userId, yesterday);

            // Generate AI-powered insights
            return generateAIInsights(healthData);

        } catch (Exception e) {
            log.error("Error generating insights for user {}: {}", userId, e.getMessage());
            return getDefaultInsights();
        }
    }

    private DailyHealthData collectDailyHealthData(Long userId, LocalDate date) {
        // i want only generate for once a day for the specific user
        DailyHealthData data = new DailyHealthData();
        data.setDate(date);

        // Get user profile for context
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            data.setGoal(profile.getGoal() != null ? profile.getGoal().getDescription() : "Unknown");
            data.setActivityLevel(profile.getActivityLevel() != null ? profile.getActivityLevel().getDescription() : "Unknown");
            data.setHealthConditions(profile.getHealthCondition() != null ? profile.getHealthCondition().getDescription() : "None");
            data.setDietaryRestrictions(profile.getDietaryRestriction() != null ? profile.getDietaryRestriction().getDescription() : "None");
        }

        // Get nutrition summary
        collectNutritionData(userId, date, data);

        // Get exercise data
        collectExerciseData(userId, date, data);

        // Get medication data
        collectMedicationData(userId, date, data);

        // Get food variety data
        collectFoodVarietyData(userId, date, data);

        return data;
    }

    private void collectNutritionData(Long userId, LocalDate date, DailyHealthData data) {
        // Try to get from nutrition summary first
        Optional<DailyNutritionSummary> summary = dailyNutritionSummaryRepository
                .findByUserIdAndDate(userId, date);

        if (summary.isPresent()) {
            DailyNutritionSummary s = summary.get();
            data.setTotalCaloriesConsumed(s.getConsumedCalories());
            data.setTotalProteinConsumed(s.getConsumedProtein());
            data.setTotalCarbsConsumed(s.getConsumedCarbs());
            data.setTotalFatConsumed(s.getConsumedFat());
            data.setTotalWaterConsumed(s.getWaterConsumedMl());
            data.setTotalCaloriesBurned(s.getCaloriesBurned());

            data.setTargetCalories(s.getTargetCalories());
            data.setTargetProtein(s.getTargetProtein());
            data.setTargetCarbs(s.getTargetCarbs());
            data.setTargetFat(s.getTargetFat());
            data.setTargetWater(s.getTargetWaterMl());
        } else {
            // Fallback: calculate from individual logs
            calculateFromLogs(userId, date, data);
        }
    }

    private void calculateFromLogs(Long userId, LocalDate date, DailyHealthData data) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // Calculate water consumption
        List<WaterLog> waterLogs = waterLogRepository
                .findByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        BigDecimal totalWater = waterLogs.stream()
                .map(WaterLog::getAmountMl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.setTotalWaterConsumed(totalWater);
        data.setTargetWater(new BigDecimal("2000")); // Default target

        // Set defaults for other values if no summary exists
        data.setTotalCaloriesConsumed(BigDecimal.ZERO);
        data.setTotalProteinConsumed(BigDecimal.ZERO);
        data.setTotalCarbsConsumed(BigDecimal.ZERO);
        data.setTotalFatConsumed(BigDecimal.ZERO);
        data.setTargetCalories(new BigDecimal("2000")); // Default targets
        data.setTargetProtein(new BigDecimal("50"));
        data.setTargetCarbs(new BigDecimal("250"));
        data.setTargetFat(new BigDecimal("65"));
    }

    private void collectExerciseData(Long userId, LocalDate date, DailyHealthData data) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ExerciseLog> exercises = exerciseLogRepository
                .findByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);

        BigDecimal totalBurned = exercises.stream()
                .map(ExerciseLog::getCaloriesBurned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.setTotalCaloriesBurned(totalBurned);

        Integer totalMinutes = exercises.stream()
                .mapToInt(ExerciseLog::getDurationMinutes)
                .sum();
        data.setTotalExerciseMinutes(totalMinutes);

        List<String> exerciseTypes = exercises.stream()
                .map(ExerciseLog::getExerciseName)
                .distinct()
                .collect(Collectors.toList());
        data.setExerciseTypes(exerciseTypes);
    }

    private void collectMedicationData(Long userId, LocalDate date, DailyHealthData data) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<MedicationLog> medicationLogs = medicationLogRepository
                .findByUserIdAndDateRange(userId, startOfDay, endOfDay);

        data.setTotalMedicationsScheduled(medicationLogs.size());

        long takenCount = medicationLogs.stream()
                .filter(log -> log.getStatus() == LogStatus.TAKEN)
                .count();
        data.setMedicationsTaken((int) takenCount);

        long missedCount = medicationLogs.stream()
                .filter(log -> log.getStatus() == LogStatus.MISSED)
                .count();
        data.setMedicationsMissed((int) missedCount);

        List<String> missedMeds = medicationLogs.stream()
                .filter(log -> log.getStatus() == LogStatus.MISSED)
                .map(log -> log.getMedication().getName())
                .collect(Collectors.toList());
        data.setMissedMedications(missedMeds);
    }

    private void collectFoodVarietyData(Long userId, LocalDate date, DailyHealthData data) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<FoodLog> foodLogs = foodLogRepository
                .findFoodLogsByUserIdAndDateRange(userId, startOfDay, endOfDay);

        data.setMealCount(foodLogs.size());

        // Get unique food names from food items
        List<String> foods = foodLogs.stream()
                .flatMap(log -> log.getItems().stream())
                .map(FoodItem::getName)
                .distinct()
                .collect(Collectors.toList());
        data.setFoodsConsumed(foods);
    }

    private InsightsResponse generateAIInsights(DailyHealthData data) {
        try {
            String userDataSummary = buildUserDataSummary(data);
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(userDataSummary);

            String aiResponse = chatClient.prompt()
                    .messages(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt)
                    )
                    .call()
                    .content();

            return parseAIResponse(aiResponse);

        } catch (Exception e) {
            log.error("Error generating AI insights: {}", e.getMessage());
            return getDefaultInsights();
        }
    }

    private String buildUserDataSummary(DailyHealthData data) {
        StringBuilder summary = new StringBuilder();
        summary.append("YESTERDAY'S HEALTH DATA ANALYSIS\n\n");

        // Basic info
        summary.append(String.format("Date: %s\n", data.getDate()));
        summary.append(String.format("User Goal: %s\n", data.getGoal()));
        summary.append(String.format("Activity Level: %s\n", data.getActivityLevel()));
        summary.append(String.format("Health Conditions: %s\n", data.getHealthConditions()));
        summary.append(String.format("Dietary Restrictions: %s\n\n", data.getDietaryRestrictions()));

        // Nutrition analysis
        summary.append("NUTRITION PERFORMANCE:\n");
        summary.append(String.format("Calories: %s/%s (%.1f%%)\n",
                formatNumber(data.getTotalCaloriesConsumed()),
                formatNumber(data.getTargetCalories()),
                calculatePercentage(data.getTotalCaloriesConsumed(), data.getTargetCalories())));
        summary.append(String.format("Protein: %sg/%sg (%.1f%%)\n",
                formatNumber(data.getTotalProteinConsumed()),
                formatNumber(data.getTargetProtein()),
                calculatePercentage(data.getTotalProteinConsumed(), data.getTargetProtein())));
        summary.append(String.format("Carbs: %sg/%sg (%.1f%%)\n",
                formatNumber(data.getTotalCarbsConsumed()),
                formatNumber(data.getTargetCarbs()),
                calculatePercentage(data.getTotalCarbsConsumed(), data.getTargetCarbs())));
        summary.append(String.format("Fat: %sg/%sg (%.1f%%)\n",
                formatNumber(data.getTotalFatConsumed()),
                formatNumber(data.getTargetFat()),
                calculatePercentage(data.getTotalFatConsumed(), data.getTargetFat())));
        summary.append(String.format("Water: %sml/%sml (%.1f%%)\n\n",
                formatNumber(data.getTotalWaterConsumed()),
                formatNumber(data.getTargetWater()),
                calculatePercentage(data.getTotalWaterConsumed(), data.getTargetWater())));

        // Exercise analysis
        summary.append("EXERCISE PERFORMANCE:\n");
        summary.append(String.format("Total Exercise Time: %d minutes\n",
                data.getTotalExerciseMinutes() != null ? data.getTotalExerciseMinutes() : 0));
        summary.append(String.format("Calories Burned: %s\n",
                formatNumber(data.getTotalCaloriesBurned())));
        if (data.getExerciseTypes() != null && !data.getExerciseTypes().isEmpty()) {
            summary.append(String.format("Exercise Types: %s\n", String.join(", ", data.getExerciseTypes())));
        }
        summary.append("\n");

        // Medication compliance
        if (data.getTotalMedicationsScheduled() != null && data.getTotalMedicationsScheduled() > 0) {
            summary.append("MEDICATION COMPLIANCE:\n");
            summary.append(String.format("Scheduled: %d, Taken: %d, Missed: %d (%.1f%% compliance)\n",
                    data.getTotalMedicationsScheduled(),
                    data.getMedicationsTaken() != null ? data.getMedicationsTaken() : 0,
                    data.getMedicationsMissed() != null ? data.getMedicationsMissed() : 0,
                    calculateMedicationCompliance(data)));
            if (data.getMissedMedications() != null && !data.getMissedMedications().isEmpty()) {
                summary.append(String.format("Missed Medications: %s\n", String.join(", ", data.getMissedMedications())));
            }
            summary.append("\n");
        }

        // Food variety
        summary.append("FOOD VARIETY:\n");
        summary.append(String.format("Total Meals Logged: %d\n",
                data.getMealCount() != null ? data.getMealCount() : 0));
        if (data.getFoodsConsumed() != null && !data.getFoodsConsumed().isEmpty()) {
            summary.append(String.format("Unique Foods: %d (%s)\n",
                    data.getFoodsConsumed().size(),
                    data.getFoodsConsumed().size() > 10 ?
                            String.join(", ", data.getFoodsConsumed().subList(0, 10)) + "..." :
                            String.join(", ", data.getFoodsConsumed())));
        }

        return summary.toString();
    }

    private String buildSystemPrompt() {
        return """
        You are HealVerse AI, an expert health and wellness advisor specializing in personalized insights.

        TASK: Analyze yesterday's health data and provide actionable insights in exactly 3 categories.

        RESPONSE FORMAT: Return ONLY a JSON object with this exact structure:
        {
          "medicationInsights": [
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" }
          ],
          "dietInsights": [
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" }
          ],
          "healthInsights": [
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" },
            { "content": "string", "type": "BETTER|SUGGESTION|WARNING|DANGER" }
          ]
        }

        RULES:
        - Each category must have exactly 3 insights.
        - "content" is a concise, actionable, personalized insight (max 150 characters).
        - "type" indicates the nature of the insight:
            BETTER → praise or positive reinforcement.
            SUGGESTION → neutral advice for improvement.
            WARNING → cautionary note about potential issues.
            DANGER → critical health warning or urgent concern.
        - Make all insights specific to the user's provided data.
        - medicationInsights: Only about medication logs & compliance.
        - dietInsights: Only about nutrition, hydration, exercise, food variety.
        - healthInsights: General wellness, lifestyle, activity, and overall health.
        - If a category has no related data, still return 3 general insights for that category.
        - Return only the JSON object, no markdown, no extra text.
        """;
    }

    private String buildUserPrompt(String userDataSummary) {
        return "Analyze this user's yesterday health data and provide personalized insights (see only medicationInsights should be given for medication logs related , dietInsights areonly for diet analysys and logs of foods , water , exercise , and healthInsights are realted to all and everything):\n\n" + userDataSummary;
    }

    private InsightsResponse parseAIResponse(String aiResponse) {
        try {
            String cleanResponse = aiResponse.trim();
            if (cleanResponse.contains("```json")) {
                cleanResponse = cleanResponse.substring(cleanResponse.indexOf("{"), cleanResponse.lastIndexOf("}") + 1);
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cleanResponse, InsightsResponse.class);

        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage(), e);
            return getDefaultInsights();
        }
    }

    // The next two methods are no longer needed but retained for backwards compatibility if you ever want to fall back to string based parsing
    /*
    private InsightsResponse parseJsonResponse(String jsonResponse) {
        InsightsResponse response = new InsightsResponse();

        try {
            // Extract medicationInsights
            List<InsightItem> medInsights = extractInsightsFromJson(jsonResponse, "medicationInsights");
            response.setMedicationInsights(medInsights);

            // Extract dietInsights
            List<InsightItem> dietInsights = extractInsightsFromJson(jsonResponse, "dietInsights");
            response.setDietInsights(dietInsights);

            // Extract healthInsights
            List<InsightItem> healthInsights = extractInsightsFromJson(jsonResponse, "healthInsights");
            response.setHealthInsights(healthInsights);

        } catch (Exception e) {
            log.error("Error parsing JSON insights: {}", e.getMessage());
            return getDefaultInsights();
        }

        return response;
    }

    private List<InsightItem> extractInsightsFromJson(String json, String key) {
        List<InsightItem> insights = new ArrayList<>();
        // A real implementation should use proper JSON parsing instead of regex
        // Here, simply fallback to default if needed
        insights.addAll(getDefaultInsightsForCategory(key));
        return insights;
    }
    */

    private List<InsightItem> getDefaultInsightsForCategory(String category) {
        switch (category) {
            case "medicationInsights":
                return Arrays.asList(
                        new InsightItem("Set medication reminders to improve consistency", "SUGGESTION"),
                        new InsightItem("Track side effects and discuss with your doctor", "SUGGESTION"),
                        new InsightItem("Consider a pill organizer for better management", "SUGGESTION")
                );
            case "dietInsights":
                return Arrays.asList(
                        new InsightItem("Increase water intake for better hydration", "SUGGESTION"),
                        new InsightItem("Add more variety to your meals for balanced nutrition", "SUGGESTION"),
                        new InsightItem("Consider smaller, frequent meals for better metabolism", "SUGGESTION")
                );
            case "healthInsights":
                return Arrays.asList(
                        new InsightItem("Aim for at least 30 minutes of daily activity", "SUGGESTION"),
                        new InsightItem("Maintain consistent sleep schedule for better recovery", "SUGGESTION"),
                        new InsightItem("Monitor your progress and celebrate small wins", "BETTER")
                );
            default:
                return Collections.singletonList(new InsightItem("Stay consistent with your health goals", "SUGGESTION"));
        }
    }

    private InsightsResponse getDefaultInsights() {
        return new InsightsResponse(
                Arrays.asList(
                        new InsightItem("Set daily reminders for your medications", "SUGGESTION"),
                        new InsightItem("Keep a health journal to track symptoms", "SUGGESTION"),
                        new InsightItem("Stay consistent with your medication schedule", "BETTER")
                ),
                Arrays.asList(
                        new InsightItem("Focus on balanced nutrition with all macronutrients", "SUGGESTION"),
                        new InsightItem("Increase your daily water intake", "SUGGESTION"),
                        new InsightItem("Plan your meals ahead for better choices", "SUGGESTION")
                ),
                Arrays.asList(
                        new InsightItem("Aim for at least 150 minutes of moderate exercise weekly", "SUGGESTION"),
                        new InsightItem("Prioritize 7-9 hours of quality sleep", "SUGGESTION"),
                        new InsightItem("Practice stress management techniques daily", "SUGGESTION")
                )
        );
    }

    // Helper methods
    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return number.setScale(1, RoundingMode.HALF_UP).toString();
    }

    private double calculatePercentage(BigDecimal consumed, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        if (consumed == null) return 0.0;
        return consumed.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private double calculateMedicationCompliance(DailyHealthData data) {
        if (data.getTotalMedicationsScheduled() == null || data.getTotalMedicationsScheduled() == 0) {
            return 0.0;
        }
        int taken = data.getMedicationsTaken() != null ? data.getMedicationsTaken() : 0;
        return ((double) taken / data.getTotalMedicationsScheduled()) * 100;
    }
}