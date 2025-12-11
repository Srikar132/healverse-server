package com.bytehealers.healverse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private Integer age;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "current_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "target_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal goal;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_loss_speed")
    private WeightLossSpeed weightLossSpeed = WeightLossSpeed.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_restriction")
    private DietaryRestriction dietaryRestriction = DietaryRestriction.NON_VEGETARIAN;


    @Enumerated(EnumType.STRING)
    @Column(name = "health_conditions")
    private HealthCondition healthCondition = HealthCondition.NONE;

    private String otherHealthConditionDescription;



    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setOtherHealthConditionDescription(String description) {
        this.otherHealthConditionDescription =
                healthCondition == HealthCondition.OTHER ? description : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}