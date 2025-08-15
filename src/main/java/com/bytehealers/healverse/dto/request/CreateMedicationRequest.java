package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.model.FrequencyType;
import com.bytehealers.healverse.model.MedicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicationRequest {
    @NotBlank(message = "Medicine name is required")
    private String name;

    @NotBlank(message = "Dosage is required")
    private String dosage;

    @NotNull(message = "Medicine type is required")
    private MedicationType type;

    @NotNull(message = "Frequency is required")
    private FrequencyType frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotEmpty(message = "At least one schedule time is required")
    private List<String> scheduleTimes; // ["08:00", "20:00"]

    private String notes;
}