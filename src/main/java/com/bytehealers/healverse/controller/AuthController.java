package com.bytehealers.healverse.controller;

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
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request) {
        User registeredUser = userService.registerUser(request.getUser(), request.getProfile());
        String token = jwtService.generateJwtToken(registeredUser);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", createUserResponse(registeredUser));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateJwtToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", createUserResponse(user));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newToken = jwtService.generateJwtToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("user", createUserResponse(user));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        // Add more fields if needed
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
