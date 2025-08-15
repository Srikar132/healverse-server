package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.model.FrequencyType;
import com.bytehealers.healverse.model.Medication;
import com.bytehealers.healverse.model.MedicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicationResponse {
    private UUID id;
    private String name;
    private String dosage;
    private MedicationType type;
    private FrequencyType frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private String notes;
    private List<ScheduleResponse> schedules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MedicationResponse from(Medication medication) {
        MedicationResponse response = new MedicationResponse();
        response.setId(medication.getId());
        response.setName(medication.getName());
        response.setDosage(medication.getDosage());
        response.setType(medication.getType());
        response.setFrequency(medication.getFrequency());
        response.setStartDate(medication.getStartDate());
        response.setEndDate(medication.getEndDate());
        response.setIsActive(medication.getIsActive());
        response.setNotes(medication.getNotes());
        response.setCreatedAt(medication.getCreatedAt());
        response.setUpdatedAt(medication.getUpdatedAt());

        response.setSchedules(
                medication.getSchedules().stream()
                        .map(ScheduleResponse::from)
                        .collect(Collectors.toList())
        );

        return response;
    }
}