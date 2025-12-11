package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyPointsDTO {
    private Long userId;
    private LocalDate date;
    private Integer loginPoints;
    private Integer dietPoints;
    private Integer totalPoints;
}