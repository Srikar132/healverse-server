package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.GamificationSummaryDTO;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.service.GamificationService;
import com.bytehealers.healverse.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private UserContext userContext;


    // Get user's gamification summary
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GamificationSummaryDTO>> getSummary() {
        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getUserSummary(userId)));
    }

//    // Get points history
//    @GetMapping("/history/{userId}")
//    public ResponseEntity<List<PointsHistory>> getHistory(
//            @PathVariable Long userId,
//            @RequestParam(required = false) Integer days) {
//        if (days == null) days = 30;
//        return ResponseEntity.ok(gamificationService.(userId, days));
//    }
}