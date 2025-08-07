package com.bytehealers.healverse.service;


import com.bytehealers.healverse.model.ExerciseIntensity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CalorieCalculatorService {

    // MET (Metabolic Equivalent) values for different exercises
    private final Map<String, Map<ExerciseIntensity, BigDecimal>> exerciseMETValues = new HashMap<>();

    public CalorieCalculatorService() {
        initializeExerciseDatabase();
    }

    private void initializeExerciseDatabase() {
        // Walking
        Map<ExerciseIntensity, BigDecimal> walking = new HashMap<>();
        walking.put(ExerciseIntensity.LOW, new BigDecimal("3.0"));      // Slow pace
        walking.put(ExerciseIntensity.MODERATE, new BigDecimal("4.3")); // Moderate pace
        walking.put(ExerciseIntensity.HIGH, new BigDecimal("5.0"));     // Brisk pace
        exerciseMETValues.put("walking", walking);

        // Running
        Map<ExerciseIntensity, BigDecimal> running = new HashMap<>();
        running.put(ExerciseIntensity.LOW, new BigDecimal("6.0"));      // Jogging
        running.put(ExerciseIntensity.MODERATE, new BigDecimal("9.8")); // 6 mph
        running.put(ExerciseIntensity.HIGH, new BigDecimal("12.3"));    // 8 mph
        exerciseMETValues.put("running", running);

        // Cycling
        Map<ExerciseIntensity, BigDecimal> cycling = new HashMap<>();
        cycling.put(ExerciseIntensity.LOW, new BigDecimal("5.8"));      // Leisure
        cycling.put(ExerciseIntensity.MODERATE, new BigDecimal("8.0")); // Moderate
        cycling.put(ExerciseIntensity.HIGH, new BigDecimal("12.0"));    // Vigorous
        exerciseMETValues.put("cycling", cycling);

        // Swimming
        Map<ExerciseIntensity, BigDecimal> swimming = new HashMap<>();
        swimming.put(ExerciseIntensity.LOW, new BigDecimal("5.8"));     // Leisure
        swimming.put(ExerciseIntensity.MODERATE, new BigDecimal("8.3")); // Moderate
        swimming.put(ExerciseIntensity.HIGH, new BigDecimal("11.0"));   // Vigorous
        exerciseMETValues.put("swimming", swimming);

        // Weight Training
        Map<ExerciseIntensity, BigDecimal> weightTraining = new HashMap<>();
        weightTraining.put(ExerciseIntensity.LOW, new BigDecimal("3.0"));     // Light
        weightTraining.put(ExerciseIntensity.MODERATE, new BigDecimal("5.0")); // Moderate
        weightTraining.put(ExerciseIntensity.HIGH, new BigDecimal("6.0"));    // Vigorous
        exerciseMETValues.put("weight training", weightTraining);

        // Yoga
        Map<ExerciseIntensity, BigDecimal> yoga = new HashMap<>();
        yoga.put(ExerciseIntensity.LOW, new BigDecimal("2.5"));       // Gentle
        yoga.put(ExerciseIntensity.MODERATE, new BigDecimal("3.0"));  // Hatha
        yoga.put(ExerciseIntensity.HIGH, new BigDecimal("4.0"));     // Power yoga
        exerciseMETValues.put("yoga", yoga);

        // Default exercise
        Map<ExerciseIntensity, BigDecimal> defaultExercise = new HashMap<>();
        defaultExercise.put(ExerciseIntensity.LOW, new BigDecimal("3.0"));
        defaultExercise.put(ExerciseIntensity.MODERATE, new BigDecimal("5.0"));
        defaultExercise.put(ExerciseIntensity.HIGH, new BigDecimal("7.0"));
        exerciseMETValues.put("default", defaultExercise);
    }

    public BigDecimal calculateExerciseCalories(String exerciseName, Integer durationMinutes, ExerciseIntensity intensity) {
        BigDecimal weightKg = new BigDecimal("70");
        BigDecimal timeHours = new BigDecimal(durationMinutes)
                .divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);

        String normalizedExercise = exerciseName.toLowerCase().trim();
        Map<ExerciseIntensity, BigDecimal> intensityMap = exerciseMETValues.get(normalizedExercise);

        if (intensityMap == null) {
            intensityMap = exerciseMETValues.get("default");
        }

        BigDecimal metValue = intensityMap.getOrDefault(intensity, new BigDecimal("5.0"));

        return metValue.multiply(weightKg).multiply(timeHours);
    }


    public Map<String, Map<ExerciseIntensity, BigDecimal>> getAllExerciseTypes() {
        return exerciseMETValues;
    }
}