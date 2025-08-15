package com.bytehealers.healverse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    @JsonIgnore
    private Medication medication;

    @Column(nullable = false)
    private LocalTime time;

    @Column
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}