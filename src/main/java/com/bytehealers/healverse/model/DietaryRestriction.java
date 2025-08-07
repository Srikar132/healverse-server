package com.bytehealers.healverse.model;

public enum DietaryRestriction {
    NON_VEGETARIAN("Non-Vegetarian"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    PESCATARIAN("Pescatarian"),
    KETO("Ketogenic"),
    PALEO("Paleo"),
    MEDITERRANEAN("Mediterranean"),
    GLUTEN_FREE("Gluten-Free"),
    DAIRY_FREE("Dairy-Free"),
    LOW_CARB("Low Carb"),
    LOW_FAT("Low Fat");

    private final String description;

    DietaryRestriction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}