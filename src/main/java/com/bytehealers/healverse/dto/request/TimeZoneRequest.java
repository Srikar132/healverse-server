package com.bytehealers.healverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TimeZoneRequest {

    // IANA zone id, e.g. "Asia/Kolkata"
    @NotBlank(message = "Time zone is required")
    @Size(max = 64, message = "Time zone cannot exceed 64 characters")
    private String timeZone;
}
