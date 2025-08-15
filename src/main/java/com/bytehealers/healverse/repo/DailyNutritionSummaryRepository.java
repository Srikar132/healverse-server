package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.DailyNutritionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyNutritionSummaryRepository extends JpaRepository<DailyNutritionSummary, Long> {

    Optional<DailyNutritionSummary> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyNutritionSummary> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT d FROM DailyNutritionSummary d WHERE d.user.id = :userId ORDER BY d.date DESC")
    List<DailyNutritionSummary> findLast7DaysSummary(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM daily_nutrition_summaries WHERE user_id = :userId ORDER BY date DESC LIMIT 7", nativeQuery = true)
    List<DailyNutritionSummary> findLast7DaysSummaryNative(@Param("userId") Long userId);

    @Query("SELECT d FROM DailyNutritionSummary d WHERE d.user.id = :userId AND d.date = CURRENT_DATE")
    Optional<DailyNutritionSummary> findTodaysSummary(@Param("userId") Long userId);

    @Query("SELECT AVG(d.consumedCalories) FROM DailyNutritionSummary d WHERE d.user.id = :userId AND d.date BETWEEN :startDate AND :endDate")
    Double getAverageCaloriesInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    default Optional<DailyNutritionSummary> findTodaysSummaryAlternative(Long userId) {
        return findByUserIdAndDate(userId, LocalDate.now());
    }



    /**
     * Find nutrition summaries for a user within a date range
     */
    @Query("SELECT dns FROM DailyNutritionSummary dns WHERE dns.user.id = :userId AND dns.date BETWEEN :startDate AND :endDate ORDER BY dns.date ASC")
    List<DailyNutritionSummary> findByUserIdAndDateBetween(@Param("userId") Long userId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    /**
     * Find the most recent nutrition summary for a user
     */
    @Query("SELECT dns FROM DailyNutritionSummary dns WHERE dns.user.id = :userId ORDER BY dns.date DESC LIMIT 1")
    Optional<DailyNutritionSummary> findMostRecentByUserId(@Param("userId") Long userId);

    /**
     * Find all summaries where user exceeded calorie targets
     */
    @Query("SELECT dns FROM DailyNutritionSummary dns WHERE dns.user.id = :userId AND dns.consumedCalories > dns.targetCalories AND dns.date BETWEEN :startDate AND :endDate")
    List<DailyNutritionSummary> findCalorieExcessDays(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Get weekly average consumption for a user
     */
    @Query("SELECT AVG(dns.consumedCalories), AVG(dns.consumedProtein), AVG(dns.consumedCarbs), AVG(dns.consumedFat) " +
            "FROM DailyNutritionSummary dns WHERE dns.user.id = :userId AND dns.date BETWEEN :startDate AND :endDate")
    Object[] getWeeklyAverages(@Param("userId") Long userId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);
}
