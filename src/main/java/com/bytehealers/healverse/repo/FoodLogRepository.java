package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.FoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    @Query("SELECT f FROM FoodLog f WHERE f.user.id = :userId AND f.loggedAt >= :startOfDay AND f.loggedAt < :endOfDay")
    List<FoodLog> findTodaysFoodLogsByUserId(@Param("userId") Long userId,
                                             @Param("startOfDay") LocalDateTime startOfDay,
                                             @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT f FROM FoodLog f WHERE f.user.id = :userId AND f.loggedAt BETWEEN :startDate AND :endDate")
    List<FoodLog> findFoodLogsByUserIdAndDateRange(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    List<FoodLog> findByUserIdOrderByLoggedAtDesc(Long userId);
}