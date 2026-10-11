package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.UserProfileDTO;
import com.bytehealers.healverse.dto.request.RegisterRequest;
import com.bytehealers.healverse.exception.DuplicateResourceException;
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
    public User registerUser(RegisterRequest request) {
        RegisterRequest.Credentials credentials = request.getUser();
        String username = credentials.getUsername().trim();
        String email = (credentials.getEmail() == null || credentials.getEmail().isBlank())
                ? null
                : credentials.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        // Always a brand-new entity: nothing from the request can address an existing row
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(credentials.getPassword()));

        User savedUser = userRepository.save(user);

        UserProfileDTO profileDTO = request.getProfile();
        if (profileDTO != null) {
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            applyProfile(profile, profileDTO);
            userProfileService.createProfile(profile);
            savedUser.setProfile(profile);
        }

        return savedUser;
    }

    private void applyProfile(UserProfile profile, UserProfileDTO dto) {
        profile.setGender(dto.getGender());
        profile.setAge(dto.getAge());
        profile.setHeightCm(dto.getHeightCm());
        profile.setCurrentWeightKg(dto.getCurrentWeightKg());
        profile.setTargetWeightKg(dto.getTargetWeightKg());
        profile.setActivityLevel(dto.getActivityLevel());
        profile.setGoal(dto.getGoal());
        profile.setWeightLossSpeed(dto.getWeightLossSpeed());
        profile.setDietaryRestriction(dto.getDietaryRestriction());
        if (dto.getHealthConditions() != null) {
            profile.setHealthCondition(dto.getHealthConditions());
        }
        profile.setOtherHealthConditionDescription(dto.getOtherHealthConditionDescription());
        // Optional fields keep their stored value when the client omits them
        if (dto.getAddress() != null) {
            profile.setAddress(dto.getAddress());
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserProfile createUserProfile(Long userId, @Valid UserProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        applyProfile(profile, profileDTO);

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
                
        applyProfile(existingProfile, profileDTO);

        return userProfileService.updateProfile(existingProfile);
    }

    public Optional<UserProfile> getUserProfileById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userProfileRepository.findByUserId(userId);
    }
}
