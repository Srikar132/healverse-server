package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.model.DietPlan;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.UserProfile;
import com.bytehealers.healverse.repo.DailyNutritionSummaryRepository;
import com.bytehealers.healverse.repo.DietPlanRepository;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DietPlanGenerationTests {

    private static final Long USER_ID = 7L;
    private static final LocalDate DAY = LocalDate.of(2026, 1, 15);
    private static final long WAIT_SECONDS = 5;

    private DietPlanRepository dietPlanRepository;
    private UserProfileRepository userProfileRepository;
    private AIRecommendationService aiRecommendationService;
    private DietPlanWriter writer;
    private DietPlanService service;
    private User user;

    @BeforeEach
    void setUp() {
        dietPlanRepository = mock(DietPlanRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        aiRecommendationService = mock(AIRecommendationService.class);
        writer = mock(DietPlanWriter.class);
        service = new DietPlanService(dietPlanRepository, userProfileRepository, aiRecommendationService,
                userRepository, mock(DailyNutritionSummaryRepository.class), writer, Clock.systemUTC());

        user = new User();
        user.setId(USER_ID);
        when(dietPlanRepository.findByUserIdAndPlanDate(USER_ID, DAY)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(new UserProfile()));
    }

    @Test
    void existingPlanIsReturnedWithoutCallingTheAi() {
        DietPlan existing = new DietPlan();
        when(dietPlanRepository.findByUserIdAndPlanDate(USER_ID, DAY)).thenReturn(Optional.of(existing));

        assertSame(existing, service.generateDailyPlan(USER_ID, DAY));

        verify(aiRecommendationService, never()).generateDietPlan(any());
        verify(writer, never()).save(anyLong(), any(), any());
    }

    @Test
    void missingProfileIsRejectedBeforeTheAiCall() {
        when(userProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.generateDailyPlan(USER_ID, DAY));

        verify(aiRecommendationService, never()).generateDietPlan(any());
    }

    @Test
    void losingTheRaceToAnotherWriterReturnsTheStoredPlan() {
        DietPlan generated = planWithMeals();
        DietPlan winner = new DietPlan();
        when(aiRecommendationService.generateDietPlan(user)).thenReturn(generated);
        when(writer.save(USER_ID, DAY, generated)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(dietPlanRepository.findByUserIdAndPlanDate(USER_ID, DAY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(winner));

        assertSame(winner, service.generateDailyPlan(USER_ID, DAY));
    }

    @Test
    void concurrentCallsForTheSameDayShareOneAiCall() throws Exception {
        DietPlan generated = planWithMeals();
        CountDownLatch aiStarted = new CountDownLatch(1);
        CountDownLatch releaseAi = new CountDownLatch(1);
        when(aiRecommendationService.generateDietPlan(user)).thenAnswer(invocation -> {
            aiStarted.countDown();
            releaseAi.await(WAIT_SECONDS, TimeUnit.SECONDS);
            return generated;
        });
        when(writer.save(USER_ID, DAY, generated)).thenReturn(generated);

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<DietPlan> owner = pool.submit(() -> service.generateDailyPlan(USER_ID, DAY));
            aiStarted.await(WAIT_SECONDS, TimeUnit.SECONDS);
            List<Future<DietPlan>> waiters = List.of(
                    pool.submit(() -> service.generateDailyPlan(USER_ID, DAY)),
                    pool.submit(() -> service.generateDailyPlan(USER_ID, DAY)));
            Thread.sleep(200);
            releaseAi.countDown();

            assertSame(generated, owner.get(WAIT_SECONDS, TimeUnit.SECONDS));
            for (Future<DietPlan> waiter : waiters) {
                assertSame(generated, waiter.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        verify(aiRecommendationService, times(1)).generateDietPlan(user);
        verify(writer, times(1)).save(USER_ID, DAY, generated);
    }

    @Test
    void aiFailureReachesTheWaiterAndDoesNotBlockTheNextAttempt() throws Exception {
        CountDownLatch aiStarted = new CountDownLatch(1);
        CountDownLatch releaseAi = new CountDownLatch(1);
        when(aiRecommendationService.generateDietPlan(user)).thenAnswer(invocation -> {
            aiStarted.countDown();
            releaseAi.await(WAIT_SECONDS, TimeUnit.SECONDS);
            throw new IllegalStateException("AI down");
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<DietPlan> owner = pool.submit(() -> service.generateDailyPlan(USER_ID, DAY));
            aiStarted.await(WAIT_SECONDS, TimeUnit.SECONDS);
            Future<DietPlan> waiter = pool.submit(() -> service.generateDailyPlan(USER_ID, DAY));
            Thread.sleep(200);
            releaseAi.countDown();

            ExecutionException ownerFailure = assertThrows(ExecutionException.class,
                    () -> owner.get(WAIT_SECONDS, TimeUnit.SECONDS));
            ExecutionException waiterFailure = assertThrows(ExecutionException.class,
                    () -> waiter.get(WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals("AI down", ownerFailure.getCause().getMessage());
            assertEquals("AI down", waiterFailure.getCause().getMessage());
        } finally {
            pool.shutdownNow();
        }

        DietPlan generated = planWithMeals();
        doReturn(generated).when(aiRecommendationService).generateDietPlan(user);
        when(writer.save(USER_ID, DAY, generated)).thenReturn(generated);

        assertSame(generated, service.generateDailyPlan(USER_ID, DAY));
    }

    private static DietPlan planWithMeals() {
        DietPlan plan = new DietPlan();
        plan.setMeals(List.of());
        return plan;
    }
}
