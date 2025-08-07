package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.MealType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FoodLogRequest {
    private MealType mealType;
    private String description;
    private LocalDateTime loggedAt;
}