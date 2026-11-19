package com.bytehealers.healverse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "food_items")
@Data
@EqualsAndHashCode(exclude = {"foodLog"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "foodLog")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_log_id")
    @JsonIgnore
    private FoodLog foodLog;

    private String name;

    private double quantity;

    private String unit; // g, ml, etc.

    private double calories;

    private double protein;

    private double fat;

    private double carbs;

}
