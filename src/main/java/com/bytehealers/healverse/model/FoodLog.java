package com.bytehealers.healverse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "food_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private MealType mealType; // BREAKFAST, LUNCH, DINNER, etc.

    private String mealName;

    private String imageUrl;

    private String imageDescription;

    private LocalDateTime loggedAt;

    private LocalDateTime createdAt;

    private Boolean isFromCamera;

    @OneToMany(mappedBy = "foodLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodItem> items;

}
