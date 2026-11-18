package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class DietPlanUniquenessTests {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

    @Autowired
    private DietPlanRepository dietPlanRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void secondPlanForTheSameUserAndDayIsRejected() {
        User user = persistUser("alice");
        dietPlanRepository.saveAndFlush(plan(user, DAY));

        assertThrows(DataIntegrityViolationException.class,
                () -> dietPlanRepository.saveAndFlush(plan(user, DAY)));
    }

    @Test
    void sameDayForDifferentUsersAndDifferentDaysForOneUserAreAllowed() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        dietPlanRepository.saveAndFlush(plan(alice, DAY));

        assertDoesNotThrow(() -> dietPlanRepository.saveAndFlush(plan(bob, DAY)));
        assertDoesNotThrow(() -> dietPlanRepository.saveAndFlush(plan(alice, DAY.plusDays(1))));
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        return entityManager.persistAndFlush(user);
    }

    private DietPlan plan(User user, LocalDate date) {
        DietPlan plan = new DietPlan();
        plan.setUser(user);
        plan.setPlanDate(date);
        plan.setTotalCalories(BigDecimal.valueOf(2000));
        plan.setTotalProtein(BigDecimal.valueOf(100));
        plan.setTotalCarbs(BigDecimal.valueOf(250));
        plan.setTotalFat(BigDecimal.valueOf(60));
        return plan;
    }
}
