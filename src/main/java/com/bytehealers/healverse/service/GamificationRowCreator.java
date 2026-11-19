package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DailyPoints;
import com.bytehealers.healverse.model.UserStreaks;
import com.bytehealers.healverse.repo.DailyPointsRepository;
import com.bytehealers.healverse.repo.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Inserts the per-user rows the gamification service updates. Each insert runs in its own
 * transaction: when two requests create the same row at once, the loser's insert fails on the unique
 * constraint and only that small transaction is rolled back. Inside the caller's transaction the
 * failed insert would leave it aborted on PostgreSQL, and every later query in it would fail too.
 */
@Component
@RequiredArgsConstructor
class GamificationRowCreator {

    private final UserStreakRepository streakRepository;

    private final DailyPointsRepository dailyPointsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void createStreak(Long userId) {
        UserStreaks streak = new UserStreaks();
        streak.setUserId(userId);
        streak.setCurrentLoginStreak(0);
        streak.setLongestLoginStreak(0);
        streak.setTotalPoints(0);
        streakRepository.saveAndFlush(streak);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void createDailyPoints(Long userId, LocalDate date) {
        DailyPoints points = new DailyPoints();
        points.setUserId(userId);
        points.setDate(date);
        points.setLoginPoints(0);
        points.setDietPoints(0);
        points.setTotalPoints(0);
        dailyPointsRepository.saveAndFlush(points);
    }
}
