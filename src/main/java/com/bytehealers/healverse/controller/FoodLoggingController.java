package com.bytehealers.healverse.controller;


import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.model.MealType;
import com.bytehealers.healverse.service.FoodLoggingService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/food-logging")
public class FoodLoggingController {

    @Autowired
    private FoodLoggingService foodLoggingService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/log/description")
    public ResponseEntity<FoodLogResponse> logFoodByDescription(@RequestBody FoodLogRequest request) {
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.logFoodByDescription(userId, request);
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/log/image")
    @PostMapping(value = "/log/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FoodLogResponse> logFoodByImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart("mealType") String mealType,
            @RequestPart(value = "loggedAt", required = false) String loggedAt) {

        System.out.println(image.getName());

        Long userId = userContext.getCurrentUserId();

        FoodLogRequest request = new FoodLogRequest();
        request.setMealType(MealType.valueOf(mealType.toUpperCase()));
        if (loggedAt != null) {
            request.setLoggedAt(LocalDateTime.parse(loggedAt));
        }

        FoodLogResponse response = foodLoggingService.logFoodByImage(userId, image, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<List<FoodLogResponse>> getTodaysFoodLogs() {
        Long userId = userContext.getCurrentUserId();
        List<FoodLogResponse> response = foodLoggingService.getTodaysFoodLogs(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<FoodLogResponse>> getAllFoodLogs() {
        Long userId = userContext.getCurrentUserId();
        List<FoodLogResponse> response = foodLoggingService.getAllFoodLogs(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodLogResponse> getFoodLogById(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.getFoodLogById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodLogResponse> updateFoodLog(
            @PathVariable Long id,
            @RequestBody FoodLogRequest request) {
        Long userId = userContext.getCurrentUserId();
        FoodLogResponse response = foodLoggingService.updateFoodLog(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFoodLog(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        foodLoggingService.deleteFoodLog(userId, id);
        return ResponseEntity.noContent().build();
    }

}
