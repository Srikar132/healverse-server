package com.bytehealers.healverse.model;

public enum Goal {
    LOSE_WEIGHT("Lose weight"),
    MAINTAIN_WEIGHT("Maintain current weight"),
    GAIN_WEIGHT("Gain weight"),
    BUILD_MUSCLE("Build muscle"),
    IMPROVE_FITNESS("Improve overall fitness");

    private final String description;

    Goal(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}