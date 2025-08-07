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
}
