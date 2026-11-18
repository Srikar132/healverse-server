
package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.enums.WaterPreset;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class QuickWaterLogRequest {
    @NotNull(message = "Water preset is required")
    private WaterPreset preset;

    private LocalDateTime loggedAt;

    // Constructors
    public QuickWaterLogRequest() {}

    // Getters and Setters
    public WaterPreset getPreset() { return preset; }
    public void setPreset(WaterPreset preset) { this.preset = preset; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}