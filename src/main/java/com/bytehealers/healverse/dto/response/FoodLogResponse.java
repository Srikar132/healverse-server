package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.model.MealType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FoodLogResponse {
    private Long id;
    private MealType mealType;
    private String mealName;
    private String imageUrl;
    private String imageDescription;
    private LocalDateTime loggedAt;
    private LocalDateTime createdAt;
    private Boolean isFromCamera;
    private List<FoodItemResponse> items;
}