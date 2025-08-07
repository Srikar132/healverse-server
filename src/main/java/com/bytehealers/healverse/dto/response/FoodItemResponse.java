package com.bytehealers.healverse.dto.response;

import lombok.Data;

@Data
public class FoodItemResponse {
    private Long id;
    private String name;
    private double quantity;
    private String unit;
    private double calories;
    private double protein;
    private double fat;
    private double carbs;
}