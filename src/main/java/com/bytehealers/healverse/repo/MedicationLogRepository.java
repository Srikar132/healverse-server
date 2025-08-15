package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.LogStatus;
import com.bytehealers.healverse.model.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, UUID> {
    List<MedicationLog> findByMedicationId(UUID medicationId);

    @Query("SELECT ml FROM MedicationLog ml WHERE ml.medication.user.id = :userId AND DATE(ml.scheduledTime) = :date")
    List<MedicationLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT ml FROM MedicationLog ml WHERE ml.medication.user.id = :userId AND ml.scheduledTime BETWEEN :startDate AND :endDate")
    List<MedicationLog> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);




    @Query("""
    SELECT l FROM MedicationLog l
    WHERE l.medication.id = :medicationId
    AND l.scheduledTime >= :startDateTime
    AND l.scheduledTime < :endDateTime
""")
    Optional<MedicationLog> findTodayLog(
            @Param("medicationId") UUID medicationId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );


    List<MedicationLog> findByStatusAndScheduledTimeBefore(
            LogStatus status,
            LocalDateTime time
    );




}