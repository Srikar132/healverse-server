package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsHistoryDTO {
    private Long id;
    private Integer pointsEarned;
    private String reason;
    private String description;
    private LocalDate date;
}

