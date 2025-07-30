package com.bytehealers.healverse.controller;


import com.bytehealers.healverse.dto.UserProfileDTO;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

//    @GetMapping("/profile")
//    public ResponseEntity<UserProfile> getUserProfile(Authentication authentication) {
//        String username = getUserIdFromAuth(authentication);
//        UserProfile profile = userService.getUserProfile();
//        return ResponseEntity.ok(profile);
//    }

//    @PostMapping("/profile")
//    public ResponseEntity<UserProfile> createUserProfile(
//            @Valid @RequestBody UserProfileDTO profileDTO,
//            Authentication authentication) {
//
//        Long userId = getUserIdFromAuth(authentication);
//        UserProfile profile = userService.createUserProfile(userId, profileDTO);
//        return ResponseEntity.ok(profile);
//    }
//
//    @PutMapping("/profile")
//    public ResponseEntity<UserProfile> updateUserProfile(
//            @Valid @RequestBody UserProfileDTO profileDTO,
//            Authentication authentication) {
//
//        Long userId = getUserIdFromAuth(authentication);
//        UserProfile profile = userService.updateUserProfile(userId, profileDTO);
//        return ResponseEntity.ok(profile);
//    }

    private String getUserIdFromAuth(Authentication authentication) {
        return authentication.getName();
    }

}
