package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.UserStreaks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreaks, Long> {

    Optional<UserStreaks> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}