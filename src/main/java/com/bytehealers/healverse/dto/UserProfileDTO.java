package com.bytehealers.healverse.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

import com.bytehealers.healverse.model.*;

@Data
public class UserProfileDTO {

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Age is required")
    @Min(value = 15, message = "Age must be at least 15")
    @Max(value = 100, message = "Age must be less than 100")
    private Integer age;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "100.0", message = "Height must be at least 100 cm")
    @DecimalMax(value = "250.0", message = "Height must be less than 250 cm")
    private BigDecimal heightCm;

    @NotNull(message = "Current weight is required")
    @DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Weight must be less than 300 kg")
    private BigDecimal currentWeightKg;

    @NotNull(message = "Target weight is required")
    @DecimalMin(value = "30.0", message = "Target weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Target weight must be less than 300 kg")
    private BigDecimal targetWeightKg;

    @NotNull(message = "Activity level is required")
    private ActivityLevel activityLevel;

    @NotNull(message = "Goal is required")
    private Goal goal;

    private WeightLossSpeed weightLossSpeed = WeightLossSpeed.MODERATE;

    private DietaryRestriction dietaryRestriction = DietaryRestriction.NON_VEGETARIAN;

    private HealthCondition healthConditions;

    private String otherHealthConditionDescription;

}