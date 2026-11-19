package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.exception.ExternalServiceException;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.repo.FoodLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodLoggingServiceTests {

    private static final Long USER_ID = 5L;
    private static final Long OTHER_USER_ID = 6L;
    private static final Long LOG_ID = 31L;
    private static final LocalDateTime LOGGED_AT = LocalDateTime.of(2026, 1, 2, 12, 30);

    private static final String AI_JSON = """
            {"mealName":"Eggs and banana","description":"Breakfast","items":[
              {"name":"Boiled egg","quantity":2,"unit":"piece","calories":156,"protein":12,"fat":10,"carbs":1},
              {"name":"Banana","quantity":1,"unit":"piece","calories":105,"protein":1,"fat":0,"carbs":27}]}
            """;

    private FoodLogRepository foodLogRepository;
    private NutritionSyncService nutritionSyncService;
    private ChatClient chatClient;
    private FoodLoggingService service;

    @BeforeEach
    void setUp() {
        foodLogRepository = mock(FoodLogRepository.class);
        nutritionSyncService = mock(NutritionSyncService.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        UserRepository userRepository = mock(UserRepository.class);
        service = new FoodLoggingService(foodLogRepository, userRepository, nutritionSyncService, chatClient,
                new ObjectMapper(), mock(GamificationService.class), Clock.systemUTC(), mock(UserTime.class));
        ReflectionTestUtils.setField(service, "reasoningEffort", "");
        ReflectionTestUtils.setField(service, "foodMaxTokens", 1200);

        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(foodLogRepository.save(any(FoodLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void aiReturns(String content) {
        when(chatClient.prompt().options(any()).user(anyString()).call().content()).thenReturn(content);
    }

    private FoodLogRequest descriptionRequest() {
        FoodLogRequest request = new FoodLogRequest();
        request.setMealType(MealType.BREAKFAST);
        request.setDescription("2 boiled eggs and a banana");
        request.setLoggedAt(LOGGED_AT);
        return request;
    }

    private FoodLog existingLog(Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        FoodLog log = new FoodLog();
        log.setId(LOG_ID);
        log.setUser(owner);
        log.setMealType(MealType.LUNCH);
        log.setLoggedAt(LOGGED_AT);
        log.setItems(new ArrayList<>());
        when(foodLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));
        return log;
    }

    @Test
    void textMealBecomesAFoodLogWithTheAnalysedItemsAndSyncsItsDay() {
        aiReturns(AI_JSON);

        FoodLogResponse response = service.logFoodByDescription(USER_ID, descriptionRequest());

        assertEquals("Eggs and banana", response.getMealName());
        assertEquals(2, response.getItems().size());
        assertEquals(MealType.BREAKFAST, response.getMealType());
        assertEquals(LOGGED_AT, response.getLoggedAt());
        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, LOGGED_AT);
    }

    @Test
    void unusableAiAnswerSavesNothingAndSyncsNothing() {
        aiReturns("not json at all");

        assertThrows(ExternalServiceException.class,
                () -> service.logFoodByDescription(USER_ID, descriptionRequest()));

        verify(foodLogRepository, never()).save(any());
        verify(nutritionSyncService, never()).syncAfterFoodLogAsync(any(), any());
    }

    @Test
    void summaryIsSyncedOnlyAfterTheTransactionCommits() {
        aiReturns(AI_JSON);
        TransactionSynchronizationManager.initSynchronization();

        service.logFoodByDescription(USER_ID, descriptionRequest());

        verify(nutritionSyncService, never()).syncAfterFoodLogAsync(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, LOGGED_AT);
    }

    @Test
    void deleteSyncsTheDayOfTheRemovedLog() {
        existingLog(USER_ID);

        service.deleteFoodLog(USER_ID, LOG_ID);

        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, LOGGED_AT);
    }

    @Test
    void anotherUsersLogCannotBeDeleted() {
        existingLog(OTHER_USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteFoodLog(USER_ID, LOG_ID));

        verify(foodLogRepository, never()).delete(any());
        verify(nutritionSyncService, never()).syncAfterFoodLogAsync(any(), any());
    }

    @Test
    void movingAMealToAnotherDaySyncsBothDays() {
        existingLog(USER_ID);
        LocalDateTime nextDay = LOGGED_AT.plusDays(1);
        FoodLogRequest request = descriptionRequest();
        request.setLoggedAt(nextDay);

        service.updateFoodLog(USER_ID, LOG_ID, request);

        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, LOGGED_AT);
        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, nextDay);
    }

    @Test
    void changingOnlyTheMealTypeSyncsTheDayOnce() {
        existingLog(USER_ID);
        FoodLogRequest request = descriptionRequest();
        request.setMealType(MealType.DINNER);

        service.updateFoodLog(USER_ID, LOG_ID, request);

        verify(nutritionSyncService).syncAfterFoodLogAsync(USER_ID, LOGGED_AT);
    }
}
