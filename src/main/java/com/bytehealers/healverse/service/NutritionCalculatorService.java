package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class NutritionCalculatorService {

    public BigDecimal calculateBMR(UserProfile profile) {
        BigDecimal weight = profile.getCurrentWeightKg();
        BigDecimal height = profile.getHeightCm();
        int age = profile.getAge();

        // Mifflin-St Jeor Equation
        if (profile.getGender() == Gender.MALE) {
            return BigDecimal.valueOf(10).multiply(weight)
                    .add(BigDecimal.valueOf(6.25).multiply(height))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(age)))
                    .add(BigDecimal.valueOf(5));
        } else {
            return BigDecimal.valueOf(10).multiply(weight)
                    .add(BigDecimal.valueOf(6.25).multiply(height))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(age)))
                    .subtract(BigDecimal.valueOf(161));
        }
    }

    public BigDecimal calculateTDEE(UserProfile profile) {
        BigDecimal bmr = calculateBMR(profile);
        BigDecimal activityMultiplier = getActivityMultiplier(profile.getActivityLevel());
        return bmr.multiply(activityMultiplier).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTargetCalories(UserProfile profile) {
        BigDecimal tdee = calculateTDEE(profile);

        switch (profile.getGoal()) {
            case WEIGHT_LOSS:
                return applyWeightLossCalories(tdee, profile.getWeightLossSpeed());
            case WEIGHT_GAIN:
                return tdee.add(BigDecimal.valueOf(500)); // +500 calories for weight gain
            case MUSCLE_GAIN:
                return tdee.add(BigDecimal.valueOf(300)); // +300 calories for muscle gain
            default:
                return tdee; // Maintain weight
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
            case SEDENTARY: return BigDecimal.valueOf(1.2);
            case LIGHTLY_ACTIVE: return BigDecimal.valueOf(1.375);
            case MODERATELY_ACTIVE: return BigDecimal.valueOf(1.55);
            case VERY_ACTIVE: return BigDecimal.valueOf(1.725);
            case EXTREMELY_ACTIVE: return BigDecimal.valueOf(1.9);
            default: return BigDecimal.valueOf(1.2);
        }
    }

    public MacroDistribution calculateMacros(BigDecimal targetCalories, Goal goal) {
        switch (goal) {
            case WEIGHT_LOSS:
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.35)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Protein 35%
                        targetCalories.multiply(BigDecimal.valueOf(0.35)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Carbs 35%
                        targetCalories.multiply(BigDecimal.valueOf(0.30)).divide(BigDecimal.valueOf(9), 0, RoundingMode.HALF_UP)  // Fat 30%
                );
            case MUSCLE_GAIN:
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.30)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Protein 30%
                        targetCalories.multiply(BigDecimal.valueOf(0.45)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Carbs 45%
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(9), 0, RoundingMode.HALF_UP)  // Fat 25%
                );
            default: // WEIGHT_GAIN, MAINTAIN_WEIGHT
                return new MacroDistribution(
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Protein 25%
                        targetCalories.multiply(BigDecimal.valueOf(0.50)).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP), // Carbs 50%
                        targetCalories.multiply(BigDecimal.valueOf(0.25)).divide(BigDecimal.valueOf(9), 0, RoundingMode.HALF_UP)  // Fat 25%
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
        public BigDecimal getProtein() { return protein; }
        public BigDecimal getCarbs() { return carbs; }
        public BigDecimal getFat() { return fat; }
    }
}