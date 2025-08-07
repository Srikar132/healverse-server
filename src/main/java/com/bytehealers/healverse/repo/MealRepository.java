package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {
}
