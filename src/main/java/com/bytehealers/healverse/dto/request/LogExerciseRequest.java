package com.bytehealers.healverse.dto.request;


import com.bytehealers.healverse.model.ExerciseIntensity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class LogExerciseRequest {
    @NotBlank(message = "Exercise name is required")
    private String exerciseName;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    @NotNull(message = "Intensity is required")
    private ExerciseIntensity intensity;

    private LocalDateTime loggedAt;

    // Constructors
    public LogExerciseRequest() {}

    public LogExerciseRequest(String exerciseName, Integer durationMinutes, ExerciseIntensity intensity) {
        this.exerciseName = exerciseName;
        this.durationMinutes = durationMinutes;
        this.intensity = intensity;
        this.loggedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public ExerciseIntensity getIntensity() { return intensity; }
    public void setIntensity(ExerciseIntensity intensity) { this.intensity = intensity; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}