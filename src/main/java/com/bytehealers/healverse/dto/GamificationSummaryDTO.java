package com.bytehealers.healverse.dto;
import com.bytehealers.healverse.model.PointsHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationSummaryDTO {
    private Long userId;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalPoints;
    private Integer todayPoints;
    private LocalDate lastLogin;
    private List<PointsHistory> recentActivity;
}