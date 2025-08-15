package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.model.LogStatus;
import com.bytehealers.healverse.model.MedicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodayMedicationResponse {
    private UUID id;
    private UUID medicationId;
    private String name;
    private String dosage;
    private MedicationType type;
    private LocalDateTime scheduledTime;
    private LogStatus status;
    private LocalDateTime actualTime;
}