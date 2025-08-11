package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.DailyNutritionSummaryRepository;
import com.bytehealers.healverse.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class NutritionSyncService {

    private static final Logger log =  LoggerFactory.getLogger(NutritionSyncService.class);

    @Autowired
    private DailyNutritionSummaryRepository dailyNutritionSummaryRepository;

    @Autowired
    private ExerciseLoggingService exerciseLoggingService;

    @Autowired
    private WaterLoggingService waterLoggingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodLoggingService foodLoggingService;

    @Autowired
    private DietPlanService  dietPlanService;

    @Autowired
    private NutritionCalculatorService  nutritionCalculatorService;

    @Transactional
    public void syncDailySummary(Long userId, LocalDate date) {
        try {
            User user = getUserOrThrow(userId);
            log.info("🔄 Starting sync for userId: {} on date: {}", userId, date);

            DailyNutritionSummary summary = getOrCreateSummary(user, date);

            List<FoodLog> foodLogs = foodLoggingService.getFoodLogsByDateRange(userId, date, date);
            BigDecimal totalCalories = BigDecimal.ZERO;
            BigDecimal totalProtein = BigDecimal.ZERO;
            BigDecimal totalCarbs = BigDecimal.ZERO;
            BigDecimal totalFat = BigDecimal.ZERO;

            log.debug("Found {} food logs for user {} on {}", foodLogs.size(), userId, date);

            for (FoodLog foodLog : foodLogs) {
                for(FoodItem item : foodLog.getItems()) {
                    totalCalories = totalCalories.add(safeGetBigDecimal(BigDecimal.valueOf(item.getCalories())));
                    totalProtein = totalProtein.add(safeGetBigDecimal(BigDecimal.valueOf(item.getProtein())));
                    totalCarbs = totalCarbs.add(safeGetBigDecimal(BigDecimal.valueOf(item.getCarbs())));
                    totalFat = totalFat.add(safeGetBigDecimal(BigDecimal.valueOf(item.getFat())));
                }
            }

            List<ExerciseLog> exerciseLogs = exerciseLoggingService.getExerciseLogsByDateRange(userId, date, date);
            BigDecimal totalCaloriesBurned = exerciseLogs.stream()
                    .map(ExerciseLog::getCaloriesBurned)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Found {} exercise logs for userId {} on {}, total burned: {}",
                    exerciseLogs.size(), userId, date, totalCaloriesBurned);

            List<WaterLog> waterLogs = waterLoggingService.getWaterLogsByDateRange(userId, date, date);
            BigDecimal totalWater = waterLogs.stream()
                    .map(WaterLog::getAmountMl)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Found {} water logs for userId {} on {}, total water: {}ml",
                    waterLogs.size(), userId, date, totalWater);

            summary.setConsumedCalories(totalCalories);

            summary.setConsumedProtein(totalProtein);
            summary.setConsumedCarbs(totalCarbs);
            summary.setConsumedFat(totalFat);

            // target + (calories burned) - (consumed) = remaining


            summary.setCaloriesBurned(totalCaloriesBurned);
            summary.setWaterConsumedMl(totalWater);
            summary.setUpdatedAt(LocalDateTime.now());

            try {
                summary.calculateRemainingCalories();
            } catch (Exception e) {
                log.warn("Failed to calculate remaining calories for userId {}: {}", userId, e.getMessage());
                BigDecimal remaining = summary.getTargetCalories()
                        .subtract(totalCalories)
                        .add(totalCaloriesBurned);
                summary.setRemainingCalories(remaining);
            }

            DailyNutritionSummary savedSummary = dailyNutritionSummaryRepository.save(summary);

            log.info("✅ Successfully synced summary for userId {}: Calories={}, Protein={}, Exercise={}, Water={}ml, Remaining={}",
                    userId, totalCalories, totalProtein, totalCaloriesBurned, totalWater,
                    savedSummary.getRemainingCalories());

        } catch (Exception e) {
            log.error("❌ Failed to sync daily summary for userId: {} on date: {}", userId, date, e);
            throw new RuntimeException("Sync failed for userId " + userId, e);
        }
    }

    @Transactional
    public void syncDailySummarySync(Long userId, LocalDate date) {
        syncDailySummary(userId, date);
    }

    @Async
    public void syncAfterFoodLogAsync(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    @Async
    public void syncAfterExerciseLogAsync(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    @Async
    public void syncAfterWaterLogAsync(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    @Transactional
    public void syncAfterFoodLog(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    @Transactional
    public void syncAfterExerciseLog(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    @Transactional
    public void syncAfterWaterLog(Long userId, LocalDateTime loggedAt) {
        syncDailySummary(userId, loggedAt.toLocalDate());
    }

    private DailyNutritionSummary getOrCreateSummary(User user, LocalDate date) {
        Optional<DailyNutritionSummary> existing =
                dailyNutritionSummaryRepository.findByUserIdAndDate(user.getId(), date);

        if (existing.isPresent()) {
            log.debug("Found existing summary for userId {} on {}", user.getId(), date);
            return existing.get();
        }

        log.debug("Creating new summary for userId {} on {}", user.getId(), date);
        DailyNutritionSummary newSummary = new DailyNutritionSummary();
        newSummary.setUser(user);
        newSummary.setDate(date);
        newSummary.setCreatedAt(LocalDateTime.now());
        newSummary.setUpdatedAt(LocalDateTime.now());

        try {
            // --- Step 1: Try getting diet plan for the date ---
            DietPlan dietPlan = dietPlanService.getDailyPlan(user.getId(), date); // Will auto-generate if not exists

            if (dietPlan != null) {
                log.debug("Using diet plan for nutrition targets");
                newSummary.setTargetCalories(dietPlan.getTotalCalories());
                newSummary.setTargetProtein(dietPlan.getTotalProtein());
                newSummary.setTargetCarbs(dietPlan.getTotalCarbs());
                newSummary.setTargetFat(dietPlan.getTotalFat());
            } else {
                log.debug("No diet plan found — calculating targets from profile");
                setTargetsFromProfile(user, newSummary);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch diet plan, falling back to nutrition calculator: {}", e.getMessage());
            setTargetsFromProfile(user, newSummary);
        }

        // Calculate remaining calories/macros initially
        newSummary.calculateRemainingCalories();

        return dailyNutritionSummaryRepository.save(newSummary);
    }

    private void setTargetsFromProfile(User user, DailyNutritionSummary summary) {
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new RuntimeException("User profile not found for userId: " + user.getId());
        }

        // Calculate target calories
        BigDecimal targetCalories = nutritionCalculatorService.calculateTargetCalories(profile);
        summary.setTargetCalories(targetCalories);

        // Calculate macros
        NutritionCalculatorService.MacroDistribution macros =
                nutritionCalculatorService.calculateMacros(targetCalories, profile.getGoal());

        summary.setTargetProtein(macros.getProtein());
        summary.setTargetCarbs(macros.getCarbs());
        summary.setTargetFat(macros.getFat());
    }



    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));
    }

    private BigDecimal safeGetBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public Optional<DailyNutritionSummary> getCurrentSummary(Long userId, LocalDate date) {
        syncDailySummarySync(userId, date);
        return dailyNutritionSummaryRepository.findByUserIdAndDate(userId, date);
    }

    public DailyNutritionSummary refreshSummary(Long userId, LocalDate date) {
        syncDailySummarySync(userId, date);
        return dailyNutritionSummaryRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new RuntimeException("Failed to create/update summary"));
    }
}
