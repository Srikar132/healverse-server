package com.bytehealers.healverse.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AIFoodResponse {
    private String mealName;
    private String description;
    private List<AIFoodItem> items;

    @Data
    public static class AIFoodItem {
        private String name;
        private double quantity;
        private String unit;
        private double calories;
        private double protein;
        private double fat;
        private double carbs;
    }
}