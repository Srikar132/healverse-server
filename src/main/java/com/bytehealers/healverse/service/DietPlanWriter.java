package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Persists an already generated plan in its own short transaction. It is a separate bean so the
 * AI call in {@link DietPlanService} stays outside any transaction while the write still goes
 * through the transactional proxy.
 */
@Component
@RequiredArgsConstructor
class DietPlanWriter {

    private final DietPlanRepository dietPlanRepository;
    private final UserRepository userRepository;

    /**
     * Saves the plan with its meals (cascaded). Throws {@code DataIntegrityViolationException}
     * when the user already has a plan for the day.
     */
    @Transactional
    public DietPlan save(Long userId, LocalDate date, DietPlan plan) {
        plan.setUser(userRepository.getReferenceById(userId));
        plan.setPlanDate(date);
        return dietPlanRepository.saveAndFlush(plan);
    }
}
