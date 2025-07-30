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
    Optional<DietPlan> findByUserUsernameAndPlanDate(String username, LocalDate planDate);


    @Query("SELECT dp FROM DietPlan dp WHERE dp.user.username = :username AND dp.planDate BETWEEN :startDate AND :endDate ORDER BY dp.planDate")
    List<DietPlan> findWeeklyPlans(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}