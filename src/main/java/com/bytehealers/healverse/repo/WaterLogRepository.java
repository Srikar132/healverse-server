package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.WaterLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface WaterLogRepository extends JpaRepository<WaterLog, Long> {

    // 🔄 Changed from username to user.id
    @Query("SELECT w FROM WaterLog w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :start AND :end")
    List<WaterLog> findByUserIdAndLoggedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT w FROM WaterLog w WHERE w.user.id = :userId AND CAST(w.loggedAt AS date) = CURRENT_DATE ORDER BY w.loggedAt DESC")
    List<WaterLog> findTodaysWaterLogs(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM WaterLog w WHERE w.user.id = :userId AND CAST(w.loggedAt AS date) = CURRENT_DATE")
    BigDecimal findTotalWaterIntakeToday(@Param("userId") Long userId);

    // Optional: if you still want the alternative method
    default List<WaterLog> findTodaysWaterLogsAlternative(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return findByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
    }

    @Query("SELECT w FROM WaterLog w " +
            "WHERE w.user.id = :userId " +
            "AND CAST(w.loggedAt AS date) = :date " +
            "ORDER BY w.loggedAt DESC")
    List<WaterLog> findWaterLogsByDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );


}
