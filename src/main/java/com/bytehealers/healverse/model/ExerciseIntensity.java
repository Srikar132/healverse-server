package com.bytehealers.healverse.model;

public enum ExerciseIntensity {
    LOW(3.0),
    MODERATE(5.0),
    HIGH(8.0),
    VERY_HIGH(12.0);

    private final double metValue;

    ExerciseIntensity(double metValue) {
        this.metValue = metValue;
    }

    public double getMetValue() {
        return metValue;
    }
}