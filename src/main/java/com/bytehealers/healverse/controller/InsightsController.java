package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.InsightsResponse;
import com.bytehealers.healverse.service.InsightsService;
import com.bytehealers.healverse.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;


    @GetMapping
    public ResponseEntity<ApiResponse<InsightsResponse>> getDailyInsights(@CurrentUserId Long userId) {
        InsightsResponse insights = insightsService.generateDailyInsights(userId);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }
}
