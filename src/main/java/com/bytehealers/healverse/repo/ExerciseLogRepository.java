package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.ExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    // ✅ Native SQL query using user_id
    @Query(value = "SELECT * FROM exercise_logs WHERE user_id = :userId AND DATE(logged_at) = CURRENT_DATE ORDER BY logged_at DESC", nativeQuery = true)
    List<ExerciseLog> findTodaysExerciseLogsNative(@Param("userId") Long userId);

    // ✅ HQL query version using entity reference
    @Query("SELECT e FROM ExerciseLog e WHERE e.user.id = :userId AND e.loggedAt BETWEEN :startOfDay AND :endOfDay ORDER BY e.loggedAt DESC")
    List<ExerciseLog> findByUserIdAndLoggedAtBetween(@Param("userId") Long userId,
                                                     @Param("startOfDay") LocalDateTime startOfDay,
                                                     @Param("endOfDay") LocalDateTime endOfDay);

    // ✅ Java-based method that uses the HQL version above
    default List<ExerciseLog> findTodaysExerciseLogs(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        return findByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
    }

    @Query("SELECT e FROM ExerciseLog e WHERE e.user.id = :userId AND DATE(e.loggedAt) = :date")
    List<ExerciseLog> findExerciseLogsByDate(@Param("userId") Long userId, @Param("date") LocalDate date);

}
