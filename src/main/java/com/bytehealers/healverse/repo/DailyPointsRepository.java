package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.DailyPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPointsRepository extends JpaRepository<DailyPoints, Long> {

    Optional<DailyPoints> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyPoints> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<DailyPoints> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT SUM(dp.totalPoints) FROM DailyPoints dp WHERE dp.userId = :userId")
    Integer getTotalPointsByUserId(@Param("userId") Long userId);

    @Query("SELECT dp FROM DailyPoints dp WHERE dp.userId = :userId AND dp.date >= :fromDate ORDER BY dp.date DESC")
    List<DailyPoints> findRecentPointsByUserId(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);
}