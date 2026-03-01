package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class NutritionCalculatorService {

    public BigDecimal calculateBMR(UserProfile profile) {
        // Input validation for safety
        validateUserProfileInputs(profile);

        BigDecimal weight = profile.getCurrentWeightKg();
        BigDecimal height = profile.getHeightCm();
        int age = profile.getAge();

        // Mifflin-St Jeor Equation
        BigDecimal bmr;
        if (profile.getGender() == Gender.MALE) {
            bmr = BigDecimal.valueOf(10).multiply(weight)
                    .add(BigDecimal.valueOf(6.25).multiply(height))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(age)))
                    .add(BigDecimal.valueOf(5));
        } else {
            bmr = BigDecimal.valueOf(10).multiply(weight)
                    .add(BigDecimal.valueOf(6.25).multiply(height))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(age)))
                    .subtract(BigDecimal.valueOf(161));
        }

        // Safety check: BMR should never be below 800 or above 4000 calories
        if (bmr.compareTo(BigDecimal.valueOf(800)) < 0) {
            throw new IllegalArgumentException(
                    "Calculated BMR is dangerously low: " + bmr + " calories. Please verify user data.");
        }
        if (bmr.compareTo(BigDecimal.valueOf(4000)) > 0) {
            throw new IllegalArgumentException(
                    "Calculated BMR is unusually high: " + bmr + " calories. Please verify user data.");
        }

        return bmr;
    }

    public BigDecimal calculateTDEE(UserProfile profile) {
        BigDecimal bmr = calculateBMR(profile);
        BigDecimal activityMultiplier = getActivityMultiplier(profile.getActivityLevel());
        return bmr.multiply(activityMultiplier).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTargetCalories(UserProfile profile) {
        BigDecimal tdee = calculateTDEE(profile);

        BigDecimal targetCalories = switch (profile.getGoal()) {
            case LOSE_WEIGHT -> applyWeightLossCalories(tdee, profile.getWeightLossSpeed());
            case GAIN_WEIGHT -> tdee.add(BigDecimal.valueOf(500)); // +500 calories for weight gain
            case BUILD_MUSCLE -> tdee.add(BigDecimal.valueOf(300)); // +300 calories for muscle gain
            default -> tdee;
        };

        // Critical safety check: Never go below 1200 calories for women or 1500 for men
        BigDecimal minimumCalories = profile.getGender() == Gender.FEMALE ? BigDecimal.valueOf(1200)
                : BigDecimal.valueOf(1500);

        if (targetCalories.compareTo(minimumCalories) < 0) {
            System.err.println("WARNING: Target calories " + targetCalories +
                    " is below safe minimum " + minimumCalories +
                    " for " + profile.getGender() + ". Using minimum instead.");
            return minimumCalories;
        }

        // Upper safety limit: Never exceed 150% of TDEE
        BigDecimal maximumCalories = tdee.multiply(BigDecimal.valueOf(1.5));
        if (targetCalories.compareTo(maximumCalories) > 0) {
            System.err.println("WARNING: Target calories " + targetCalories +
                    " exceeds safe maximum " + maximumCalories + ". Using maximum instead.");
            return maximumCalories;
        }

        return targetCalories;
    }

    /**
     * Validates user profile inputs to prevent dangerous calorie calculations
     */
    private void validateUserProfileInputs(UserProfile profile) {
        if (profile.getCurrentWeightKg() == null || profile.getCurrentWeightKg().compareTo(BigDecimal.valueOf(20)) < 0
                ||
                profile.getCurrentWeightKg().compareTo(BigDecimal.valueOf(300)) > 0) {
            throw new IllegalArgumentException("Invalid weight: " + profile.getCurrentWeightKg() +
                    "kg. Weight must be between 20-300kg.");
        }

        if (profile.getHeightCm() == null || profile.getHeightCm().compareTo(BigDecimal.valueOf(100)) < 0 ||
                profile.getHeightCm().compareTo(BigDecimal.valueOf(250)) > 0) {
            throw new IllegalArgumentException("Invalid height: " + profile.getHeightCm() +
                    "cm. Height must be between 100-250cm.");
        }

        if (profile.getAge() < 12 || profile.getAge() > 120) {
            throw new IllegalArgumentException("Invalid age: " + profile.getAge() +
                    ". Age must be between 12-120 years.");
        }

        if (profile.getGender() == null) {
            throw new IllegalArgumentException("Gender is required for BMR calculation.");
        }
    }

    private BigDecimal applyWeightLossCalories(BigDecimal tdee, WeightLossSpeed speed) {
        switch (speed) {
            case SLOW:
                return tdee.subtract(BigDecimal.valueOf(250)); // -250 calories
            case FAST:
                return tdee.subtract(BigDecimal.valueOf(750)); // -750 calories
            default:
                return tdee.subtract(BigDecimal.valueOf(500)); // -500 calories (moderate)
        }
    }

    private BigDecimal getActivityMultiplier(ActivityLevel level) {
        switch (level) {
            case SEDENTARY:
                return BigDecimal.valueOf(1.2);
            case LIGHTLY_ACTIVE:
                return BigDecimal.valueOf(1.375);
            case MODERATELY_ACTIVE:
                return BigDecimal.valueOf(1.55);
            case VERY_ACTIVE:
                return BigDecimal.valueOf(1.725);
            case EXTREMELY_ACTIVE:
                return BigDecimal.valueOf(1.9);
            default:
                return BigDecimal.valueOf(1.2);
        }
    }

    public MacroDistribution calculateMacros(BigDecimal targetCalories, Goal goal) {
        switch (goal) {
            case LOSE_WEIGHT:
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.35)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Protein 35%
                        targetCalories.multiply(BigDecimal.valueOf(0.35)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Carbs 35%
                        targetCalories.multiply(BigDecimal.valueOf(0.30)).divide(BigDecimal.valueOf(9), 0,
                                RoundingMode.HALF_UP) // Fat 30%
                );
            case BUILD_MUSCLE:
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.30)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Protein 30%
                        targetCalories.multiply(BigDecimal.valueOf(0.45)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Carbs 45%
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(9), 0,
                                RoundingMode.HALF_UP) // Fat 25%
                );
            default: // WEIGHT_GAIN, MAINTAIN_WEIGHT
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Protein 25%
                        targetCalories.multiply(BigDecimal.valueOf(0.50)).divide(BigDecimal.valueOf(4), 0,
                                RoundingMode.HALF_UP), // Carbs 50%
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(9), 0,
                                RoundingMode.HALF_UP) // Fat 25%
                );
        }
    }

    public static class MacroDistribution {
        private final BigDecimal protein;
        private final BigDecimal carbs;
        private final BigDecimal fat;

        public MacroDistribution(BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }

        // Getters
        public BigDecimal getProtein() {
            return protein;
        }

        public BigDecimal getCarbs() {
            return carbs;
        }

        public BigDecimal getFat() {
            return fat;
        }
    }
}