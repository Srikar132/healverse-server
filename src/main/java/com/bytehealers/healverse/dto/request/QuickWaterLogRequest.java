
package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.WaterPreset;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class QuickWaterLogRequest {
    @NotNull(message = "Water preset is required")
    private WaterPreset preset;

    private LocalDateTime loggedAt;

    // Constructors
    public QuickWaterLogRequest() {}

    public QuickWaterLogRequest(WaterPreset preset) {
        this.preset = preset;
        this.loggedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public WaterPreset getPreset() { return preset; }
    public void setPreset(WaterPreset preset) { this.preset = preset; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}