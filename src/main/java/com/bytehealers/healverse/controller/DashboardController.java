package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.NutritionStats;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.DashboardResponse;
import com.bytehealers.healverse.model.DailyNutritionSummary;
import com.bytehealers.healverse.service.DashboardService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DashboardResponse>> getTodaysDashboard() {
        Long userId = userContext.getCurrentUserId();
        DashboardResponse dashboard = dashboardService.getTodaysDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success("Today's dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        Long userId = userContext.getCurrentUserId();
        DashboardResponse dashboard = dashboardService.getDashboard(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<DailyNutritionSummary>>> getWeeklySummary() {
        Long userId = userContext.getCurrentUserId();
        List<DailyNutritionSummary> weeklySummary = dashboardService.getWeeklySummary(userId);
        return ResponseEntity.ok(ApiResponse.success("Weekly summary retrieved successfully", weeklySummary));
    }

    @GetMapping("/summary/{date}")
    public ResponseEntity<ApiResponse<DailyNutritionSummary>> getSummaryByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = userContext.getCurrentUserId();
        DailyNutritionSummary summary = dashboardService.getSummaryByDate(userId, date);
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved successfully", summary));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<NutritionStats>> getNutritionStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long userId = userContext.getCurrentUserId();
        NutritionStats stats = dashboardService.getNutritionStats(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Nutrition stats retrieved successfully", stats));
    }
}
