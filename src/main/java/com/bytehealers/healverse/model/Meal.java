package com.bytehealers.healverse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;


@Entity
@Table(name = "meals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "dietPlan")
public class Meal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "diet_plan_id", nullable = false)
    @JsonIgnore
    private DietPlan dietPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "meal_name", nullable = false)
    private String mealName;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal calories;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal protein;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal carbs;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal fat;

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes = 0;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "health_benefits", columnDefinition = "TEXT")
    private String healthBenefits;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

//    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<String> ingredients;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
