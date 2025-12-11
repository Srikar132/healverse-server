package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.PointsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {

    List<PointsHistory> findByUserIdOrderByDateDesc(Long userId);

    List<PointsHistory> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<PointsHistory> findByUserIdAndDateAfter(Long userId, LocalDate afterDate);

    @Query("SELECT ph FROM PointsHistory ph WHERE ph.userId = :userId AND ph.date >= :fromDate ORDER BY ph.createdAt DESC")
    List<PointsHistory> findRecentHistory(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT ph FROM PointsHistory ph WHERE ph.userId = :userId ORDER BY ph.createdAt DESC")
    List<PointsHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}