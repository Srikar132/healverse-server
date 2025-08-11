package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public  class NutritionStats {
    private BigDecimal averageCalories;
    private BigDecimal totalCalories;
    private int daysCount;
}