package com.bytehealers.healverse.model;

public enum ActivityLevel {
    SEDENTARY("Sedentary (little/no exercise)"),
    LIGHTLY_ACTIVE("Lightly active (light exercise/sports 1-3 days/week)"),
    MODERATELY_ACTIVE("Moderately active (moderate exercise/sports 3-5 days/week)"),
    VERY_ACTIVE("Very active (hard exercise/sports 6-7 days a week)"),
    EXTREMELY_ACTIVE("Extremely active (very hard exercise/sports & physical job)");

    private final String description;

    ActivityLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
