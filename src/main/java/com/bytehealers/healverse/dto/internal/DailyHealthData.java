package com.bytehealers.healverse.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyHealthData {
    private LocalDate date;

    // Nutrition data
    private BigDecimal totalCaloriesConsumed;
    private BigDecimal totalProteinConsumed;
    private BigDecimal totalCarbsConsumed;
    private BigDecimal totalFatConsumed;
    private BigDecimal totalWaterConsumed;

    // Target vs actual
    private BigDecimal targetCalories;
    private BigDecimal targetProtein;
    private BigDecimal targetCarbs;
    private BigDecimal targetFat;
    private BigDecimal targetWater;

    // Exercise data
    private BigDecimal totalCaloriesBurned;
    private Integer totalExerciseMinutes;
    private List<String> exerciseTypes;

    // Medication compliance
    private Integer totalMedicationsScheduled;
    private Integer medicationsTaken;
    private Integer medicationsMissed;
    private List<String> missedMedications;

    // Food variety
    private List<String> foodsConsumed;
    private Integer mealCount;

    // User profile context
    private String goal;
    private String activityLevel;
    private String healthConditions;
    private String dietaryRestrictions;
    private String address;
}