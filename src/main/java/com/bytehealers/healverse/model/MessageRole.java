package com.bytehealers.healverse.model;

public enum MessageRole {
    USER("USER"),
    BOT("BOT");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}