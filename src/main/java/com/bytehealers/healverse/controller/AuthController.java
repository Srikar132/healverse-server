package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.service.JwtService;
import com.bytehealers.healverse.service.UserService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody @Valid RegisterRequest request) {

        User registeredUser = userService.registerUser(request.getUser(), request.getProfile());
        String token = jwtService.generateJwtToken(registeredUser);

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateJwtToken(user);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("user", createUserResponse(user));

        return ResponseEntity.ok(ApiResponse.success("Login successful", responseData));
    }

    @GetMapping("/check-auth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newToken = jwtService.generateJwtToken(user);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", newToken);
        responseData.put("user", createUserResponse(user));

        return ResponseEntity.ok(ApiResponse.success("Auth check successful", responseData));
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


    @Setter
    @Getter
    public static class RegisterRequest {
        // Getters and setters
        private User user;
        private UserProfile profile;

    }

    @Setter
    @Getter
    public static class LoginRequest {
        // Getters and setters
        private String username;
        private String password;

    }
}
