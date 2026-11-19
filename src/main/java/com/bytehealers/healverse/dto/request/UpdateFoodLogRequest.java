package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.enums.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateFoodLogRequest {

    @NotNull(message = "Meal type is required")
    private MealType mealType;

    /** Blank or missing keeps the current name. */
    @Size(max = 100, message = "Meal name must be at most 100 characters")
    private String mealName;

    /** Missing keeps the current time. */
    private LocalDateTime loggedAt;

    /** Replaces all items of the log. */
    @NotNull(message = "Items are required")
    @Size(min = 1, max = 30, message = "A meal needs between 1 and 30 items")
    private List<@NotNull(message = "Item must not be null") @Valid Item> items;

    @Data
    public static class Item {

        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name must be at most 100 characters")
        private String name;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0", inclusive = false, message = "Quantity must be greater than 0")
        @DecimalMax(value = "100000", message = "Quantity must be at most 100000")
        private Double quantity;

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must be at most 20 characters")
        private String unit;

        @NotNull(message = "Calories are required")
        @DecimalMin(value = "0", message = "Calories must be between 0 and 20000")
        @DecimalMax(value = "20000", message = "Calories must be between 0 and 20000")
        private Double calories;

        @NotNull(message = "Protein is required")
        @DecimalMin(value = "0", message = "Protein must be between 0 and 5000")
        @DecimalMax(value = "5000", message = "Protein must be between 0 and 5000")
        private Double protein;

        @NotNull(message = "Fat is required")
        @DecimalMin(value = "0", message = "Fat must be between 0 and 5000")
        @DecimalMax(value = "5000", message = "Fat must be between 0 and 5000")
        private Double fat;

        @NotNull(message = "Carbs are required")
        @DecimalMin(value = "0", message = "Carbs must be between 0 and 5000")
        @DecimalMax(value = "5000", message = "Carbs must be between 0 and 5000")
        private Double carbs;
    }
}
