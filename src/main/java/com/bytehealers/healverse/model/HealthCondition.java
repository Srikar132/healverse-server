package com.bytehealers.healverse.model;

public enum HealthCondition {
    NONE("No health conditions"),
    DIABETES("Diabetes"),
    HYPERTENSION("High Blood Pressure"),
    HEART_DISEASE("Heart Disease"),
    THYROID("Thyroid Issues"),
    PCOS("PCOS"),
    ARTHRITIS("Arthritis"),
    DIGESTIVE_ISSUES("Digestive Issues"),
    ALLERGIES("Food Allergies"),
    OTHER("Other");

    private final String description;

    HealthCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}