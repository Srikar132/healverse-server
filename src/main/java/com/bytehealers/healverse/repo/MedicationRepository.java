package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByUserIdAndIsActive(Long user_id, Boolean isActive);
    List<Medication> findByUserId(Long user_id);

    @Query("SELECT m FROM Medication m WHERE m.user.id = :userId AND m.isActive = true")
    List<Medication> findActiveByUserId(@Param("userId") Long userId);


}

