package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.UserProfileDTO;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.service.UserService;
import com.bytehealers.healverse.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfile>> getUserProfile(@CurrentUserId Long userId) {
        UserProfile profile = userService.getUserProfileById(userId).orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " not found"));

        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfile>> createUserProfile(@Valid @RequestBody UserProfileDTO profileDTO, @CurrentUserId Long userId) {
        UserProfile profile = userService.createUserProfile(userId, profileDTO);
        return ResponseEntity.ok(ApiResponse.success("User profile created successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfile>> updateUserProfile(@Valid @RequestBody UserProfileDTO profileDTO, @CurrentUserId Long userId) {
        UserProfile profile = userService.updateUserProfile(userId, profileDTO);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", profile));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Long>> getCurrentUserId(@CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Current user ID retrieved successfully", userId));
    }
}
