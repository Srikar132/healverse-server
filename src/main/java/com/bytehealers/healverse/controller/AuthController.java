package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.LoginRequest;
import com.bytehealers.healverse.dto.request.RegisterRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.service.GamificationService;
import com.bytehealers.healverse.service.JwtService;
import com.bytehealers.healverse.service.LoginAttemptService;
import com.bytehealers.healverse.service.UserPrinciple;
import com.bytehealers.healverse.service.UserService;
import com.bytehealers.healverse.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final GamificationService gamificationService;
    private final LoginAttemptService loginAttemptService;
    private final UserContext userContext;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody @Valid RegisterRequest request) {

        User registeredUser = userService.registerUser(request);
        String token = jwtService.generateJwtToken(registeredUser);

        recordDailyLogin(registeredUser.getId());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("user", createUserResponse(registeredUser));

        ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(
                "User registered successfully",
                responseData
        );

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientAddress = httpRequest.getRemoteAddr();
        loginAttemptService.checkAllowed(clientAddress, request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(clientAddress, request.getUsername());
            throw e;
        }
        loginAttemptService.recordSuccess(clientAddress, request.getUsername());

        User user = ((UserPrinciple) authentication.getPrincipal()).getUser();

        String token = jwtService.generateJwtToken(user);

        recordDailyLogin(user.getId());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("user", createUserResponse(user));

        return ResponseEntity.ok(ApiResponse.success("Login successful", responseData));
    }

    /**
     * Validates the presented token (the JWT filter has already done so) and returns the current user.
     * The same token is echoed back rather than re-issued: minting a fresh token on every call would
     * let a token be refreshed forever.
     */
    @GetMapping("/check-auth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(@RequestHeader("Authorization") String authHeader) {
        User user = userService.findById(userContext.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", authHeader.substring("Bearer ".length()));
        responseData.put("user", createUserResponse(user));

        return ResponseEntity.ok(ApiResponse.success("Auth check successful", responseData));
    }

    // Daily-login points are a side effect: a failure there must never block signing in or registering.
    private void recordDailyLogin(Long userId) {
        try {
            gamificationService.recordDailyLogin(userId);
        } catch (Exception e) {
            log.warn("Daily login points failed for userId {}: {}", userId, e.getMessage());
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        userMap.put("googleId", user.getGoogleId());
        userMap.put("profile", user.getProfile());
        return userMap;
    }
}
