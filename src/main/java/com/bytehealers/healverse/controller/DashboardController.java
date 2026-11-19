package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.NutritionStats;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.DashboardResponse;
import com.bytehealers.healverse.model.DailyNutritionSummary;
import com.bytehealers.healverse.service.DashboardService;
import com.bytehealers.healverse.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DashboardResponse>> getTodaysDashboard(@CurrentUserId Long userId) {
        DashboardResponse dashboard = dashboardService.getTodaysDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success("Today's dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @CurrentUserId Long userId
    ) {
        DashboardResponse dashboard = dashboardService.getDashboard(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<DailyNutritionSummary>>> getWeeklySummary(@CurrentUserId Long userId) {
        List<DailyNutritionSummary> weeklySummary = dashboardService.getWeeklySummary(userId);
        return ResponseEntity.ok(ApiResponse.success("Weekly summary retrieved successfully", weeklySummary));
    }

    @GetMapping("/summary/{date}")
    public ResponseEntity<ApiResponse<DailyNutritionSummary>> getSummaryByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date, @CurrentUserId Long userId) {
        DailyNutritionSummary summary = dashboardService.getSummaryByDate(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved successfully", summary));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<NutritionStats>> getNutritionStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate, @CurrentUserId Long userId) {
        NutritionStats stats = dashboardService.getNutritionStats(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Nutrition stats retrieved successfully", stats));
    }
}
