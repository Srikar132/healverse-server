package com.bytehealers.healverse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "diet_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DietPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false , name = "plan_date")
    private LocalDate planDate;


    @Column(name = "total_calories", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "total_protein", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalProtein;

    @Column(name = "total_carbs", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalCarbs;

    @Column(name = "total_fat", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalFat;

    @Column(name = "is_generated")
    private Boolean isGenerated = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "dietPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Meal> meals;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
