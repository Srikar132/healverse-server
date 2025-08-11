package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.model.DailyNutritionSummary;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.model.WaterLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class DashboardResponse {
    private DailyNutritionSummary summary;
    private List<FoodLogResponse> foodLogs;
    private List<ExerciseLog> exerciseLogs;
    private List<WaterLog> waterLogs;
}