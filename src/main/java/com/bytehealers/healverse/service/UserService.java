package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.UserProfileDTO;
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
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get().getProfile();
        } else {
            return null;
        }
    }

    public UserProfile createUserProfile(Long userId, @Valid UserProfileDTO profileDTO) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            UserProfile profile = new UserProfile();
            profile.setUser(user.get());
            profile.setGender(profileDTO.getGender());
            profile.setAge(profileDTO.getAge());
            profile.setHeightCm(profileDTO.getHeightCm());
            profile.setCurrentWeightKg(profileDTO.getCurrentWeightKg());
            profile.setActivityLevel(profileDTO.getActivityLevel());
            profile.setGoal(profileDTO.getGoal());

            user.get().setProfile(profile);

            return userProfileService.createProfile(profile);
        }

        return null;
    }

    public User findById(Long userId) {
        return  userRepository.findById(userId).orElse(null);
    }

    public UserProfile updateUserProfile(Long userId, @Valid UserProfileDTO profileDTO) {
//        return userProfileRepository.save()

        return null;
    }

    public UserProfile getUserProfileById(Long userId) {
        return userProfileRepository.findById(userId).orElse(null);
    }
}
