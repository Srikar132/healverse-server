package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.UserProfileDTO;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfileService userProfileService;


    @Transactional
    public User registerUser(User user, UserProfile profile) {
        // Encode password if provided
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Save user first
        User savedUser = userRepository.save(user);

        // Set the user reference in profile and save
        if (profile != null) {
            profile.setUser(savedUser);
            userProfileService.createProfile(profile);
            savedUser.setProfile(profile);
        }

        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    public UserProfile getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(User::getProfile)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public UserProfile createUserProfile(Long userId, @Valid UserProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setGender(profileDTO.getGender());
        profile.setAge(profileDTO.getAge());
        profile.setHeightCm(profileDTO.getHeightCm());
        profile.setCurrentWeightKg(profileDTO.getCurrentWeightKg());
        profile.setTargetWeightKg(profileDTO.getTargetWeightKg());
        profile.setActivityLevel(profileDTO.getActivityLevel());
        profile.setGoal(profileDTO.getGoal());
        profile.setWeightLossSpeed(profileDTO.getWeightLossSpeed());
        profile.setDietaryRestriction(profileDTO.getDietaryRestriction());
        profile.setHealthCondition(profileDTO.getHealthConditions());
        profile.setOtherHealthConditionDescription(profileDTO.getOtherHealthConditionDescription());

        user.setProfile(profile);
        return userProfileService.createProfile(profile);
    }

    public Optional<User> findById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    @Transactional
    public UserProfile updateUserProfile(Long userId, @Valid UserProfileDTO profileDTO) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));
                
        // Update profile fields
        existingProfile.setGender(profileDTO.getGender());
        existingProfile.setAge(profileDTO.getAge());
        existingProfile.setHeightCm(profileDTO.getHeightCm());
        existingProfile.setCurrentWeightKg(profileDTO.getCurrentWeightKg());
        existingProfile.setTargetWeightKg(profileDTO.getTargetWeightKg());
        existingProfile.setActivityLevel(profileDTO.getActivityLevel());
        existingProfile.setGoal(profileDTO.getGoal());
        existingProfile.setWeightLossSpeed(profileDTO.getWeightLossSpeed());
        existingProfile.setDietaryRestriction(profileDTO.getDietaryRestriction());
        existingProfile.setHealthCondition(profileDTO.getHealthConditions());
        existingProfile.setOtherHealthConditionDescription(profileDTO.getOtherHealthConditionDescription());
        
        return userProfileService.updateProfile(existingProfile);
    }

    public Optional<UserProfile> getUserProfileById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userProfileRepository.findByUserId(userId);
    }
}
