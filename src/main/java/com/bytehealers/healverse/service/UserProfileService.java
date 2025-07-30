package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.repo.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;


    @Transactional
    public UserProfile createProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public Optional<UserProfile> findByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    @Transactional
    public UserProfile updateProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public void deleteProfile(Long profileId) {
        userProfileRepository.deleteById(profileId);
    }

    public Optional<UserProfile> findById(Long id) {
        return userProfileRepository.findById(id);
    }
}
