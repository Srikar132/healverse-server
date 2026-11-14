package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.BadRequestException;
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
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.logFoodByDescription(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Food log created successfully", response));
    }

    @PostMapping(value = "/log/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FoodLogResponse>> logFoodByImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart("mealType") String mealType) {
        Long userId = userContext.getCurrentUserId();
        FoodLogRequest request = new FoodLogRequest();
        request.setMealType(parseMealType(mealType));

        FoodLogResponse response = foodLoggingService.logFoodByImage(userId, image, request);
        return ResponseEntity.ok(ApiResponse.success("Food log created from image successfully", response));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getFoodLogsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        Long userId = userContext.getCurrentUserId();
        List<FoodLogResponse> response = foodLoggingService.getFoodLogsByDate(userId , date);
        return ResponseEntity.ok(ApiResponse.success("Today's food logs retrieved successfully", response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getAllFoodLogs() {
        Long userId = userContext.getCurrentUserId();
        List<FoodLogResponse> response = foodLoggingService.getAllFoodLogs(userId);
        return ResponseEntity.ok(ApiResponse.success("All food logs retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> getFoodLogById(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.getFoodLogById(userId, id);
        if (response == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Food log retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> updateFoodLog(
            @PathVariable Long id,
            @RequestBody FoodLogRequest request) {
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.updateFoodLog(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Food log updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFoodLog(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        boolean deleted = foodLoggingService.deleteFoodLog(userId, id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Food log deleted successfully", "Deleted"));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
        }
    }

    private static MealType parseMealType(String mealType) {
        try {
            return MealType.valueOf(mealType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid meal type: " + mealType);
        }
    }
}
