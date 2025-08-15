package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.InsightsResponse;
//import com.bytehealers.healverse.service.InsightsService;
import com.bytehealers.healverse.service.InsightsService;
import com.bytehealers.healverse.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @Autowired
    private UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<InsightsResponse>> getDailyInsights() {
        Long userId = userContext.getCurrentUserId();
        InsightsResponse insights = insightsService.generateDailyInsights(userId);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }
}
