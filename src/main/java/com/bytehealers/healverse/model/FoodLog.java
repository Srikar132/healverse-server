package com.bytehealers.healverse.model;

import com.bytehealers.healverse.model.enums.MealType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "food_logs")
@Data
@EqualsAndHashCode(exclude = {"user", "items"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "items"})
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
