package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.NutritionStats;
import com.bytehealers.healverse.dto.response.DashboardResponse;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.DailyNutritionSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private DailyNutritionSummaryRepository dailyNutritionSummaryRepository;

    @Autowired
    private FoodLoggingService foodLoggingService;

    @Autowired
    private ExerciseLoggingService exerciseLoggingService;

    @Autowired
    private WaterLoggingService waterLoggingService;

    @Autowired
    private NutritionSyncService nutritionSyncService;

    @Autowired
    private UserService userService;

    public DashboardResponse getTodaysDashboard(Long userId) {
        LocalDate today = LocalDate.now();

       return getDashboard(userId, today);
    }

    public List<DailyNutritionSummary> getWeeklySummary(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        // Sync each day in the range to ensure fresh data
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            nutritionSyncService.syncDailySummarySync(userId, date);
        }

        return dailyNutritionSummaryRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate);
    }

    public DailyNutritionSummary getSummaryByDate(Long userId, LocalDate date) {
        // Sync data for the requested date first
        nutritionSyncService.syncDailySummarySync(userId, date);



        return dailyNutritionSummaryRepository.findByUserIdAndDate(userId, date)
                .orElse(null);
    }

    public NutritionStats getNutritionStats(Long userId, LocalDate startDate, LocalDate endDate) {


        // Sync all dates in range first
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            nutritionSyncService.syncDailySummarySync(userId, date);
        }

        List<DailyNutritionSummary> summaries = dailyNutritionSummaryRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate);

        return calculateNutritionStats(summaries);
    }

    private NutritionStats calculateNutritionStats(List<DailyNutritionSummary> summaries) {
        if (summaries.isEmpty()) {
            return new NutritionStats();
        }

        BigDecimal totalCalories = summaries.stream()
                .map(summary -> summary.getConsumedCalories() != null ? summary.getConsumedCalories() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCalories = totalCalories.divide(new BigDecimal(summaries.size()), 2, BigDecimal.ROUND_HALF_UP);

        return new NutritionStats(avgCalories, totalCalories, summaries.size());
    }

    public DashboardResponse getDashboard(Long userId, LocalDate date) {

        log.info("🎯 Getting dashboard for user: {} on date: {}", userId, date);

        Optional<DailyNutritionSummary> summary_ =
                dailyNutritionSummaryRepository.findByUserIdAndDate(userId, date);


        if (summary_.isEmpty() && date.isBefore(LocalDate.now())) {
            log.warn("Cannot generate nutrition summary for past date: {} (today is {})", date, LocalDate.now());
            return null;
        }

        // 🔥 FORCE SYNC FIRST - This ensures fresh data
        nutritionSyncService.syncDailySummarySync(userId, date);

        // Get today's logs
        List<FoodLogResponse> todayFoodLogs = foodLoggingService.getFoodLogsByDate(userId, date);
        List<ExerciseLog> todayExerciseLogs = exerciseLoggingService.getExerciseLogsByDate(userId, date);
        List<WaterLog> todayWaterLogs = waterLoggingService.getWaterLogsByDate(userId, date);

        // Get the synced summary
        Optional<DailyNutritionSummary> summaryOpt =
                dailyNutritionSummaryRepository.findByUserIdAndDate(userId, date);

        DailyNutritionSummary summary;
        if (summaryOpt.isPresent()) {
            summary = summaryOpt.get();
            log.info("✅ Retrieved synced summary: Calories={}, Exercise={}, Water={}ml",
                    summary.getConsumedCalories(), summary.getCaloriesBurned(), summary.getWaterConsumedMl());
        } else {
            // Create empty summary if none exists
            User user = userService.findById(userId);
            summary = new DailyNutritionSummary(user, date);
            log.warn("⚠️ No summary found after sync, created empty summary");
        }

        return new DashboardResponse(summary, todayFoodLogs, todayExerciseLogs, todayWaterLogs);
    }
}