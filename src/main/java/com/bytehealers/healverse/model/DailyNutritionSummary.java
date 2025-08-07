package com.bytehealers.healverse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_nutrition_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
public class DailyNutritionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "target_calories", nullable = false, precision = 6, scale = 2)
    private BigDecimal targetCalories = new BigDecimal("2000");

    @Column(name = "target_protein", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetProtein = new BigDecimal("150");

    @Column(name = "target_carbs", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetCarbs = new BigDecimal("250");

    @Column(name = "target_fat", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetFat = new BigDecimal("67");

    @Column(name = "consumed_calories", precision = 6, scale = 2)
    private BigDecimal consumedCalories = new BigDecimal("0");

    @Column(name = "consumed_protein", precision = 5, scale = 2)
    private BigDecimal consumedProtein = new BigDecimal("0");

    @Column(name = "consumed_carbs", precision = 5, scale = 2)
    private BigDecimal consumedCarbs = new BigDecimal("0");

    @Column(name = "consumed_fat", precision = 5, scale = 2)
    private BigDecimal consumedFat = new BigDecimal("0");

    @Column(name = "calories_burned", precision = 6, scale = 2)
    private BigDecimal caloriesBurned = new BigDecimal("0");

    @Column(name = "water_consumed_ml", precision = 8, scale = 2)
    private BigDecimal waterConsumedMl = new BigDecimal("0");

    @Column(name = "target_water_ml", precision = 8, scale = 2)
    private BigDecimal targetWaterMl = new BigDecimal("2000");

    @Column(name = "remaining_calories", precision = 6, scale = 2)
    private BigDecimal remainingCalories;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === Business Logic ===

    public void calculateRemainingCalories() {
        this.remainingCalories = targetCalories.subtract(consumedCalories).add(caloriesBurned);
    }

    public BigDecimal getCalorieProgress() {
        if (targetCalories.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consumedCalories.divide(targetCalories, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }

    public BigDecimal getProteinProgress() {
        if (targetProtein.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consumedProtein.divide(targetProtein, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }

    public BigDecimal getCarbsProgress() {
        if (targetCarbs.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consumedCarbs.divide(targetCarbs, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }

    public BigDecimal getFatProgress() {
        if (targetFat.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consumedFat.divide(targetFat, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }

    public BigDecimal getWaterProgress() {
        if (targetWaterMl.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return waterConsumedMl.divide(targetWaterMl, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateRemainingCalories();
    }

    // Update setters with logic
    public void setConsumedCalories(BigDecimal consumedCalories) {
        this.consumedCalories = consumedCalories;
        calculateRemainingCalories();
    }

    public void setCaloriesBurned(BigDecimal caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
        calculateRemainingCalories();
    }
}
