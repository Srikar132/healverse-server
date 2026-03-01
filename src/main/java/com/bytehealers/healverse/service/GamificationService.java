package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.GamificationSummaryDTO;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class GamificationService {

    @Autowired
    private UserStreakRepository streakRepository;

    @Autowired
    private DailyPointsRepository dailyPointsRepository;

    @Autowired
    private PointsHistoryRepository historyRepository;

    @Autowired
    private DietPlanService dietPlanService;

    // === LOGIN STREAK ===

    public void recordDailyLogin(Long userId) {
        LocalDate today = LocalDate.now();
        UserStreaks streak = getOrCreateStreak(userId);

        // Check if already logged in today
        if (today.equals(streak.getLastLoginDate())) {
            return; // Already recorded
        }

        // Check if streak continues or breaks
        LocalDate yesterday = today.minusDays(1);
        if (yesterday.equals(streak.getLastLoginDate())) {
            // Continue streak
            streak.setCurrentLoginStreak(streak.getCurrentLoginStreak() + 1);
        } else {
            // Streak broken, reset
            streak.setCurrentLoginStreak(1);
        }

        // Update longest streak
        if (streak.getCurrentLoginStreak() > streak.getLongestLoginStreak()) {
            streak.setLongestLoginStreak(streak.getCurrentLoginStreak());
        }

        streak.setLastLoginDate(today);
        streakRepository.save(streak);

        // Award login points
        int loginPoints = 10;
        awardPoints(userId, loginPoints, PointReason.DAILY_LOGIN, "Daily login bonus");

        // Check for milestone bonuses
        checkStreakMilestones(userId, streak.getCurrentLoginStreak());
    }

    private void checkStreakMilestones(Long userId, int streakDays) {
        int bonusPoints = 0;
        String milestone = "";

        if (streakDays == 3) {
            bonusPoints = 50;
            milestone = "3-day streak";
        } else if (streakDays == 7) {
            bonusPoints = 100;
            milestone = "7-day streak";
        } else if (streakDays == 14) {
            bonusPoints = 250;
            milestone = "14-day streak";
        } else if (streakDays == 30 || (streakDays > 30 && streakDays % 30 == 0)) {
            bonusPoints = 500;
            milestone = streakDays + "-day streak";
        }

        if (bonusPoints > 0) {
            awardPoints(userId, bonusPoints, PointReason.STREAK_MILESTONE, milestone + " bonus");
        }
    }

    // === DIET FOLLOWING ===

    public void recordFoodLog(Long userId, FoodLog foodLog) {
        LocalDate today = LocalDate.now();

        // Get today's diet plan for user
        DietPlan todaysPlan = dietPlanService.getTodaysDietPlan(userId);
        if (todaysPlan == null) {
            return; // No diet plan to compare
        }

        // Find suggested meal for this meal type
        Meal suggestedMeal = findSuggestedMeal(todaysPlan, foodLog.getMealType());
        if (suggestedMeal == null) {
            return;
        }

        // Calculate similarity
        int similarityScore = calculateMealSimilarity(suggestedMeal, foodLog);

        // Award points based on similarity
        int pointsEarned = 0;
        if (similarityScore >= 70) {
            pointsEarned = 30;
        } else if (similarityScore >= 50) {
            pointsEarned = 15;
        } else if (similarityScore >= 30) {
            pointsEarned = 5;
        }

        if (pointsEarned > 0) {
            awardPoints(userId, pointsEarned, PointReason.DIET_FOLLOW,
                    "Followed " + foodLog.getMealType() + " plan (" + similarityScore + "% match)");

            // Check if all 3 meals logged and matched
            checkDailyDietCompletion(userId, today);
        }
    }

    private int calculateMealSimilarity(Meal suggestedMeal, FoodLog foodLog) {
        if (suggestedMeal == null || foodLog == null) {
            return 0;
        }

        // Basic similarity calculation - can be enhanced with more sophisticated logic
        int similarity = 0;

        // Check meal type match (30% weight)
        if (suggestedMeal.getMealType() == foodLog.getMealType()) {
            similarity += 30;
        }

        // Check meal name similarity (40% weight)
        if (suggestedMeal.getMealName() != null && foodLog.getMealName() != null) {
            String suggestedName = suggestedMeal.getMealName().toLowerCase();
            String loggedName = foodLog.getMealName().toLowerCase();

            if (suggestedName.equals(loggedName)) {
                similarity += 40;
            } else if (suggestedName.contains(loggedName) || loggedName.contains(suggestedName)) {
                similarity += 20;
            } else {
                // Check for common food keywords
                String[] keywords = { "chicken", "rice", "vegetables", "salad", "fish", "pasta", "soup" };
                for (String keyword : keywords) {
                    if (suggestedName.contains(keyword) && loggedName.contains(keyword)) {
                        similarity += 10;
                        break;
                    }
                }
            }
        }

        // Check nutritional similarity (30% weight)
        if (foodLog.getItems() != null && !foodLog.getItems().isEmpty()) {
            // For now, give some points if food items are logged
            similarity += 15;
        }

        return Math.min(similarity, 100); // Cap at 100%
    }

    private void checkDailyDietCompletion(Long userId, LocalDate date) {
        // Check if user has logged all 3 meals with good matches today
        int mealsLogged = countMatchedMealsToday(userId, date);

        if (mealsLogged >= 3) {
            DailyPoints dailyPoints = getDailyPoints(userId, date);
            if (dailyPoints != null && dailyPoints.getTotalPoints() >= 45) { // At least 15 points per meal
                awardPoints(userId, 50, PointReason.DAILY_COMPLETE, "Completed full day of diet plan");
            }
        }
    }

    private int countMatchedMealsToday(Long userId, LocalDate date) {
        // Count points history entries for diet following today
        List<PointsHistory> todayDietPoints = historyRepository
                .findByUserIdAndDateBetween(userId, date, date)
                .stream()
                .filter(ph -> PointReason.DIET_FOLLOW.name().equals(ph.getReason()))
                .toList();
        
        return Math.min(todayDietPoints.size(), 3); // Max 3 meals per day
    }    // === HELPER METHODS ===

    @Transactional
    private UserStreaks getOrCreateStreak(Long userId) {
        // Try to find existing first
        Optional<UserStreaks> existingStreak = streakRepository.findByUserId(userId);
        if (existingStreak.isPresent()) {
            return existingStreak.get();
        }

        // Create new streak with retry logic for race conditions
        try {
            UserStreaks newStreak = new UserStreaks();
            newStreak.setUserId(userId);
            newStreak.setCurrentLoginStreak(0);
            newStreak.setLongestLoginStreak(0);
            newStreak.setLastLoginDate(null);
            newStreak.setTotalPoints(0);
            return streakRepository.save(newStreak);
        } catch (Exception e) {
            // Handle potential race condition - another thread created the record
            Optional<UserStreaks> retryStreak = streakRepository.findByUserId(userId);
            if (retryStreak.isPresent()) {
                return retryStreak.get();
            }
            throw new RuntimeException("Failed to create or retrieve UserStreaks for userId: " + userId, e);
        }
    }

    @Transactional
    private DailyPoints getOrCreateDailyPoints(Long userId, LocalDate date) {
        // Try to find existing first
        Optional<DailyPoints> existing = dailyPointsRepository.findByUserIdAndDate(userId, date);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new daily points with retry logic for race conditions
        try {
            DailyPoints newPoints = new DailyPoints();
            newPoints.setUserId(userId);
            newPoints.setDate(date);
            newPoints.setLoginPoints(0);
            newPoints.setDietPoints(0);
            newPoints.setTotalPoints(0);
            return dailyPointsRepository.save(newPoints);
        } catch (Exception e) {
            // Handle potential race condition - unique constraint on (userId, date)
            Optional<DailyPoints> retryPoints = dailyPointsRepository.findByUserIdAndDate(userId, date);
            if (retryPoints.isPresent()) {
                return retryPoints.get();
            }
            throw new RuntimeException("Failed to create or retrieve DailyPoints for userId: " + userId + ", date: " + date, e);
        }
    }

    private DailyPoints getDailyPoints(Long userId, LocalDate date) {
        return dailyPointsRepository.findByUserIdAndDate(userId, date).orElse(null);
    }

    private Meal findSuggestedMeal(DietPlan dietPlan, MealType mealType) {
        if (dietPlan == null || dietPlan.getMeals() == null) {
            return null;
        }

        return dietPlan.getMeals().stream()
                .filter(meal -> meal.getMealType() == mealType)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    private void awardPoints(Long userId, int points, PointReason reason, String description) {
        LocalDate today = LocalDate.now();

        // Update daily points
        DailyPoints dailyPoints = getOrCreateDailyPoints(userId, today);
        if (reason == PointReason.DAILY_LOGIN) {
            dailyPoints.setLoginPoints(dailyPoints.getLoginPoints() + points);
        } else {
            dailyPoints.setDietPoints(dailyPoints.getDietPoints() + points);
        }
        dailyPoints.setTotalPoints(dailyPoints.getLoginPoints() + dailyPoints.getDietPoints());
        dailyPointsRepository.save(dailyPoints);

        // Update total points in streak table
        UserStreaks streak = getOrCreateStreak(userId);
        streak.setTotalPoints(streak.getTotalPoints() + points);
        streakRepository.save(streak);

        // Record in history
        PointsHistory history = new PointsHistory();
        history.setId(null); // Explicitly set ID to null for auto-generation
        history.setUserId(userId);
        history.setPointsEarned(points);
        history.setReason(reason.name());
        history.setDescription(description);
        history.setDate(today);
        // Don't set createdAt - let @CreationTimestamp handle it
        historyRepository.save(history);
    }

    public GamificationSummaryDTO getUserSummary(Long userId) {
        UserStreaks streak = getOrCreateStreak(userId);
        LocalDate today = LocalDate.now();
        DailyPoints todayPoints = getDailyPoints(userId, today);
        List<PointsHistory> recentHistory = historyRepository
                .findByUserIdAndDateAfter(userId, today.minusDays(7));

        return GamificationSummaryDTO.builder()
                .longestStreak(streak.getLongestLoginStreak())
                .totalPoints(streak.getTotalPoints())
                .userId(streak.getUserId())
                .currentStreak(streak.getCurrentLoginStreak())
                .todayPoints(todayPoints.getTotalPoints())
                .recentActivity(recentHistory)
                .lastLogin(streak.getLastLoginDate())
                .build();
    }

    // === ADDITIONAL PUBLIC METHODS ===

    public DailyPoints getTodayPoints(Long userId) {
        return getDailyPoints(userId, LocalDate.now());
    }

    public List<PointsHistory> getUserPointsHistory(Long userId, int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days);
        return historyRepository.findByUserIdAndDateAfter(userId, fromDate);
    }

    public UserStreaks getUserStreaks(Long userId) {
        return getOrCreateStreak(userId);
    }
}