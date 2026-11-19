package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.DailyPoints;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.FoodItem;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.Meal;
import com.bytehealers.healverse.model.UserStreaks;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.repo.DailyPointsRepository;
import com.bytehealers.healverse.repo.UserStreakRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The gamification service against a real transaction manager and database (the in-memory test
 * one): what the mocked unit tests cannot show is how its transactions behave around commits.
 */
@SpringBootTest
class GamificationTransactionTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 2);

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private UserStreakRepository streakRepository;

    @Autowired
    private DailyPointsRepository dailyPointsRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private DietPlanService dietPlanService;

    @MockitoBean
    private UserTime userTime;

    private static Long newUserId() {
        return ThreadLocalRandom.current().nextLong(1_000_000L, Long.MAX_VALUE);
    }

    @Test
    void pointsRequestedAfterCommitAreSavedInTheirOwnTransaction() {
        Long userId = newUserId();
        when(userTime.today(userId)).thenReturn(TODAY);
        DietPlan plan = new DietPlan();
        Meal suggested = new Meal();
        suggested.setMealType(MealType.BREAKFAST);
        suggested.setMealName("Oatmeal");
        suggested.setDietPlan(plan);
        plan.setMeals(List.of(suggested));
        when(dietPlanService.findExistingPlan(userId, TODAY)).thenReturn(Optional.of(plan));
        FoodLog logged = new FoodLog();
        logged.setMealType(MealType.BREAKFAST);
        logged.setMealName("Oatmeal");
        logged.setItems(List.of(new FoodItem()));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        gamificationService.recordFoodLog(userId, logged);
                    }
                }));

        DailyPoints saved = dailyPointsRepository.findByUserIdAndDate(userId, TODAY).orElseThrow();
        assertEquals(30, saved.getDietPoints());
        assertEquals(30, streakRepository.findByUserId(userId).orElseThrow().getTotalPoints());
    }

    @Test
    void requestsCreatingTheSameFirstRowAtOnceAllSucceedAndLeaveOneRow() throws Exception {
        Long userId = newUserId();
        when(userTime.today(userId)).thenReturn(TODAY);
        int requests = 8;
        CyclicBarrier together = new CyclicBarrier(requests);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                Callable<Integer> request = () -> {
                    together.await();
                    return gamificationService.getUserSummary(userId).getTotalPoints();
                };
                results.add(pool.submit(request));
            }
            for (Future<Integer> result : results) {
                assertEquals(0, result.get());
            }
        } finally {
            pool.shutdownNow();
        }

        List<UserStreaks> rows = streakRepository.findAll().stream()
                .filter(row -> userId.equals(row.getUserId()))
                .toList();
        assertTrue(rows.size() == 1, "expected one streak row but found " + rows.size());
    }
}
