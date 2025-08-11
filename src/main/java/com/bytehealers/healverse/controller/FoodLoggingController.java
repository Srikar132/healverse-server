package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.model.MealType;
import com.bytehealers.healverse.service.FoodLoggingService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
@CrossOrigin(origins = "*")
public class FoodLoggingController {

    @Autowired
    private FoodLoggingService foodLoggingService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/log/description")
    public ResponseEntity<ApiResponse<FoodLogResponse>> logFoodByDescription(@RequestBody FoodLogRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            FoodLogResponse response = foodLoggingService.logFoodByDescription(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Food log created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to log food: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/log/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FoodLogResponse>> logFoodByImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart("mealType") String mealType) {
        try {
            Long userId = userContext.getCurrentUserId();
            FoodLogRequest request = new FoodLogRequest();
            request.setMealType(MealType.valueOf(mealType.toUpperCase()));

            FoodLogResponse response = foodLoggingService.logFoodByImage(userId, image, request);
            return ResponseEntity.ok(ApiResponse.success("Food log created from image successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid meal type: " + mealType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to log food by image: " + e.getMessage()));
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getFoodLogsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            Long userId = userContext.getCurrentUserId();
            List<FoodLogResponse> response = foodLoggingService.getFoodLogsByDate(userId , date);
            return ResponseEntity.ok(ApiResponse.success("Today's food logs retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve today's food logs: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getAllFoodLogs() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<FoodLogResponse> response = foodLoggingService.getAllFoodLogs(userId);
            return ResponseEntity.ok(ApiResponse.success("All food logs retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve food logs: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> getFoodLogById(@PathVariable Long id) {
        try {
            Long userId = userContext.getCurrentUserId();
            FoodLogResponse response = foodLoggingService.getFoodLogById(userId, id);
            if (response == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
            }
            return ResponseEntity.ok(ApiResponse.success("Food log retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve food log: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> updateFoodLog(
            @PathVariable Long id,
            @RequestBody FoodLogRequest request) {
        try {
            Long userId = userContext.getCurrentUserId();
            FoodLogResponse response = foodLoggingService.updateFoodLog(userId, id, request);
            return ResponseEntity.ok(ApiResponse.success("Food log updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update food log: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFoodLog(@PathVariable Long id) {
        try {
            Long userId = userContext.getCurrentUserId();
            boolean deleted = foodLoggingService.deleteFoodLog(userId, id);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Food log deleted successfully", "Deleted"));
            } else {
                return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete food log: " + e.getMessage()));
        }
    }
}
