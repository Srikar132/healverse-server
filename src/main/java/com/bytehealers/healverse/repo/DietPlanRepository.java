package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

    // ✅ Valid: Get a specific diet plan by username and date
    Optional<DietPlan> findByUser_UsernameAndPlanDate(String username, LocalDate planDate);

    // ✅ Valid: Get all diet plans for a user in a given date range
    List<DietPlan> findByUser_UsernameAndPlanDateBetween(String username, LocalDate startDate, LocalDate endDate);

    // ✅ Optional: Custom query for weekly plans with ordering
    @Query("SELECT dp FROM DietPlan dp WHERE dp.user.username = :username AND dp.planDate BETWEEN :startDate AND :endDate ORDER BY dp.planDate")
    List<DietPlan> findWeeklyPlansByUsernameAndDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    Optional<DietPlan> findByUserIdAndPlanDate(Long userId, LocalDate date);

    @Query("SELECT dp FROM DietPlan dp WHERE dp.user.id = :userId AND dp.planDate BETWEEN :startDate AND :endDate ORDER BY dp.planDate")
    List<DietPlan> findWeeklyPlansByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
