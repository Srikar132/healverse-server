package com.bytehealers.healverse.model;

public enum WeightLossSpeed {
    SLOW("0.25 kg per week"),
    MODERATE("0.5 kg per week"),
    FAST("0.75 kg per week"),
    VERY_FAST("1 kg per week");

    private final String description;

    WeightLossSpeed(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
