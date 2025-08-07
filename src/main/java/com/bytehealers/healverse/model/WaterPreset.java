package com.bytehealers.healverse.model;


public enum WaterPreset {
    GLASS(250),
    BOTTLE(500),
    LARGE(750);

    private final int milliliters;

    WaterPreset(int milliliters) {
        this.milliliters = milliliters;
    }

    public int getMilliliters() {
        return milliliters;
    }
}

