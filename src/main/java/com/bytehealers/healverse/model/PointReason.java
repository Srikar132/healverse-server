package com.bytehealers.healverse.model;

public enum PointReason {
    DAILY_LOGIN("Daily login bonus"),
    DIET_FOLLOW("Following diet plan"),
    STREAK_MILESTONE("Login streak milestone"),
    DAILY_COMPLETE("Completed full day diet plan"),
    EXERCISE_LOG("Exercise activity logged"),
    WATER_INTAKE("Water intake goal met");
    
    private final String description;
    
    PointReason(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
