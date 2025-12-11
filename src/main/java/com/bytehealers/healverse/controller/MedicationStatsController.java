package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.DashboardStatsResponse;
import com.bytehealers.healverse.dto.response.TodayMedicationResponse;
import com.bytehealers.healverse.service.MedicationAnalyticsService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medication-dashboard")
public class MedicationStatsController {

    private final UserContext userContext;
    private final MedicationAnalyticsService analyticsService;

    public MedicationStatsController(UserContext userContext,
                                     MedicationAnalyticsService analyticsService) {
        this.userContext = userContext;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        Long userId = userContext.getCurrentUserId();
        DashboardStatsResponse stats = analyticsService.getDashboardStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TodayMedicationResponse>>> getTodayMedications() {
        Long userId = userContext.getCurrentUserId();
        List<TodayMedicationResponse> medications = analyticsService.getTodayMedications(userId);
        return ResponseEntity.ok(ApiResponse.success(medications));
    }


}
