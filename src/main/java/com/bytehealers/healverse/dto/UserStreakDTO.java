package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakDTO {
    private Long userId;
    private Integer currentLoginStreak;
    private Integer longestLoginStreak;
    private LocalDate lastLoginDate;
    private Integer totalPoints;
}
