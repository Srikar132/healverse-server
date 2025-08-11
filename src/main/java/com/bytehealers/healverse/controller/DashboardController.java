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
        try {
            Long userId = userContext.getCurrentUserId();
            DashboardResponse dashboard = dashboardService.getTodaysDashboard(userId);
            return ResponseEntity.ok(ApiResponse.success("Today's dashboard retrieved successfully", dashboard));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve dashboard: " + e.getMessage()));
        }
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            Long userId = userContext.getCurrentUserId();
            DashboardResponse dashboard = dashboardService.getDashboard(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve dashboard: " + e.getMessage()));
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<DailyNutritionSummary>>> getWeeklySummary() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<DailyNutritionSummary> weeklySummary = dashboardService.getWeeklySummary(userId);
            return ResponseEntity.ok(ApiResponse.success("Weekly summary retrieved successfully", weeklySummary));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve weekly summary: " + e.getMessage()));
        }
    }

    @GetMapping("/summary/{date}")
    public ResponseEntity<ApiResponse<DailyNutritionSummary>> getSummaryByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            Long userId = userContext.getCurrentUserId();
            DailyNutritionSummary summary = dashboardService.getSummaryByDate(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Summary retrieved successfully", summary));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve summary: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<NutritionStats>> getNutritionStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            NutritionStats stats = dashboardService.getNutritionStats(userId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Nutrition stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve nutrition stats: " + e.getMessage()));
        }
    }
}
