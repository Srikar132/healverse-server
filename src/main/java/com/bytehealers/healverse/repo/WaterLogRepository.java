package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.WaterLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterLogRepository extends JpaRepository<WaterLog, Long> {

    Optional<WaterLog> findByIdAndUser_Id(Long id, Long userId);

    // 🔄 Changed from username to user.id
    @Query("SELECT w FROM WaterLog w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :start AND :end ORDER BY w.loggedAt DESC")
    List<WaterLog> findByUserIdAndLoggedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );



}
