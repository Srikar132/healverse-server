package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.service.FoodLoggingService;
import com.bytehealers.healverse.service.IdempotencyService;
import com.bytehealers.healverse.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FoodLoggingController {

    private final FoodLoggingService foodLoggingService;
    private final IdempotencyService idempotencyService;


    /**
     * Send a photo ({@code image}), a text description ({@code description}), or both; at least one
     * is required. {@code loggedAt} is optional (ISO-8601). An offline client sends the time the meal
     * was logged, so a queued meal is stored on the day it was made, not the day it was uploaded.
     * {@code /log/image} is the path older app versions still call.
     */
    @PostMapping(value = {"/log", "/log/image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FoodLogResponse>> logFood(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("mealType") String mealType,
            @RequestPart(value = "loggedAt", required = false) String loggedAt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @CurrentUserId Long userId) {
        FoodLogRequest request = new FoodLogRequest();
        request.setMealType(parseMealType(mealType));
        request.setDescription(description);
        request.setLoggedAt(parseLoggedAt(loggedAt));

        FoodLogResponse response = idempotencyService.execute(userId, idempotencyKey, FoodLogResponse.class,
                () -> foodLoggingService.logFood(userId, image, request));
        return ResponseEntity.ok(ApiResponse.success("Food log created successfully", response));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getFoodLogsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @CurrentUserId Long userId
    ) {
        List<FoodLogResponse> response = foodLoggingService.getFoodLogsByDate(userId , date);
        return ResponseEntity.ok(ApiResponse.success("Today's food logs retrieved successfully", response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FoodLogResponse>>> getAllFoodLogs(@CurrentUserId Long userId) {
        List<FoodLogResponse> response = foodLoggingService.getAllFoodLogs(userId);
        return ResponseEntity.ok(ApiResponse.success("All food logs retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> getFoodLogById(@PathVariable Long id, @CurrentUserId Long userId) {
        FoodLogResponse response = foodLoggingService.getFoodLogById(userId, id);
        if (response == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Food log retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodLogResponse>> updateFoodLog(
            @PathVariable Long id,
            @RequestBody FoodLogRequest request, @CurrentUserId Long userId) {
        FoodLogResponse response = foodLoggingService.updateFoodLog(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Food log updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFoodLog(@PathVariable Long id, @CurrentUserId Long userId) {
        boolean deleted = foodLoggingService.deleteFoodLog(userId, id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Food log deleted successfully", "Deleted"));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error("Food log not found"));
        }
    }

    /**
     * Accepts an instant ("...Z" or with an offset, stored as UTC like the rest of the app) or a
     * plain local date-time. Blank means "not sent": the service then uses the current time.
     */
    static LocalDateTime parseLoggedAt(String loggedAt) {
        if (loggedAt == null || loggedAt.isBlank()) {
            return null;
        }
        String value = loggedAt.trim();
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException notAnInstant) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid loggedAt: " + loggedAt);
            }
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
