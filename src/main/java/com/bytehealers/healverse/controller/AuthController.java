package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.service.JwtService;
import com.bytehealers.healverse.service.MyUserDetailsService;
import com.bytehealers.healverse.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {


    @Autowired
    private UserService userService;


    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody  RegisterRequest request) {
        try {

            System.out.println(request);


            // Check if username already exists
            if (userService.existsByUsername(request.getUser().getUsername())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Username already exists"));
            }

            if (request.getUser().getEmail() != null &&
                    userService.existsByEmail(request.getUser().getEmail())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email already exists"));
            }


            User registeredUser = userService.registerUser(request.getUser(), request.getProfile());

            String token = jwtService.generateJwtToken(registeredUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("token", token);
            response.put("user", createUserResponse(registeredUser));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            if(authentication.isAuthenticated()) {
                String token = jwtService.generateJwtToken(request.getUsername());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("token", token);

                userService.findByUsername(request.getUsername())
                        .ifPresent(user -> response.put("user", createUserResponse(user)));

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/check-auth")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);


            if (username != null && jwtService.isTokenValid(token, userDetails)) {

                String newToken = jwtService.generateJwtToken(username);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Token refreshed successfully");
                response.put("token", newToken);
                response.put("user", createUserResponse(userService.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"))));

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }

//
//    @GetMapping("/me")
//    public User getCurrentUser() {
//        try {
//            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//            String username;
//
//            if (principal instanceof UserDetails) {
//                username = ((UserDetails) principal).getUsername();
//            } else {
//                username = principal.toString();
//            }
//
//            return userService.findByUsername(username)
//                    .orElseThrow(() -> new Error("User not found"));
//        }catch (Exception e) {
//            throw new ResourceNotFoundException("User not found");
//        }
//
//    }

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


    // Helper methods
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();

        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("profileImage", user.getProfileImage());
        userResponse.put("createdAt", user.getCreatedAt());
        userResponse.put("updatedAt", user.getUpdatedAt());
        userResponse.put("profile", user.getProfile());

        return userResponse;
    }
}
