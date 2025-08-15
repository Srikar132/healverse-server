package com.bytehealers.healverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Existing fields...
    private int adherenceRate;
    private int currentStreak;
    private int totalMedications;
    private int todayTaken;
    private int todayTotal;
    private List<TodayMedicationResponse> todayMedications;

    // New analytics fields
    private WeeklyAdherenceData weeklyAdherence;
    private List<DailyIntakeData> dailyIntake;
    private MedicineStatusData medicineStatus;

}







