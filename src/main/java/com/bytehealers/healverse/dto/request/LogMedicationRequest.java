package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.LogStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogMedicationRequest {
    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    @NotNull(message = "Status is required")
    private LogStatus status;

    private LocalDateTime actualTime;
    private String notes;
}