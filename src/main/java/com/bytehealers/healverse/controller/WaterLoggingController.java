package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.LogWaterRequest;
import com.bytehealers.healverse.dto.request.QuickWaterLogRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.service.WaterLoggingService;
import com.bytehealers.healverse.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/water-logs")
public class WaterLoggingController {

    @Autowired
    private WaterLoggingService waterLoggingService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<WaterLog>> logWater(@Valid @RequestBody LogWaterRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            WaterLog waterLog = waterLoggingService.logWater(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Water intake logged successfully", waterLog));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to log water intake: " + e.getMessage()));
        }
    }

    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<WaterLog>> logQuickWater(@Valid @RequestBody QuickWaterLogRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            WaterLog waterLog = waterLoggingService.logQuickWater(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Quick water intake logged successfully", waterLog));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to log quick water intake: " + e.getMessage()));
        }
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<WaterLog>>> getTodaysWaterLogs() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<WaterLog> waterLogs = waterLoggingService.getTodaysWaterLogs(userId);
            return ResponseEntity.ok(ApiResponse.success("Today's water logs retrieved successfully", waterLogs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve water logs: " + e.getMessage()));
        }
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<List<WaterLog>>> getWaterLogsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<WaterLog> waterLogs = waterLoggingService.getWaterLogsByDate(userId, date);
            return ResponseEntity.ok(ApiResponse.success("Today's water logs retrieved successfully", waterLogs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve water logs: " + e.getMessage()));
        }
    }

    @GetMapping("/today/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalWaterIntakeToday() {
        try {
            Long userId = userContext.getCurrentUserId();
            BigDecimal totalIntake = waterLoggingService.getTotalWaterIntakeToday(userId);
            return ResponseEntity.ok(ApiResponse.success("Total water intake retrieved successfully", totalIntake));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve total water intake: " + e.getMessage()));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<WaterLog>>> getWaterLogsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<WaterLog> waterLogs = waterLoggingService.getWaterLogsByDateRange(userId ,  startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Water logs retrieved successfully", waterLogs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve water logs: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{waterLogId}")
    public ResponseEntity<ApiResponse<String>> deleteWaterLog(@PathVariable Long waterLogId) {
        try {
            Long userId = userContext.getCurrentUserId();
            boolean deleted = waterLoggingService.deleteWaterLog(waterLogId, userId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Water log deleted successfully", "Deleted"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete water log: " + e.getMessage()));
        }
    }
}