package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.UpdateWaterLogRequest;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.repo.UserRepository;
import com.bytehealers.healverse.repo.WaterLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaterLoggingServiceTests {

    private static final Long USER_ID = 5L;
    private static final Long OTHER_USER_ID = 6L;
    private static final Long LOG_ID = 11L;
    private static final LocalDateTime LOGGED_AT = LocalDateTime.of(2026, 1, 2, 8, 30);

    private WaterLogRepository waterLogRepository;
    private NutritionSyncService nutritionSyncService;
    private WaterLoggingService service;

    @BeforeEach
    void setUp() {
        waterLogRepository = mock(WaterLogRepository.class);
        nutritionSyncService = mock(NutritionSyncService.class);
        service = new WaterLoggingService(waterLogRepository, nutritionSyncService,
                mock(UserRepository.class), Clock.systemUTC(), mock(UserTime.class));
        when(waterLogRepository.save(any(WaterLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private WaterLog existingLog() {
        return new WaterLog(LOG_ID, new User(), new BigDecimal("250"), LOGGED_AT, LOGGED_AT);
    }

    private UpdateWaterLogRequest request(String amountMl) {
        UpdateWaterLogRequest request = new UpdateWaterLogRequest();
        request.setAmountMl(new BigDecimal(amountMl));
        return request;
    }

    @Test
    void updateChangesOnlyTheAmountAndSyncsTheLoggedDay() {
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));

        WaterLog updated = service.updateWaterLog(LOG_ID, request("500"), USER_ID);

        assertEquals(new BigDecimal("500"), updated.getAmountMl());
        assertEquals(LOGGED_AT, updated.getLoggedAt());
        verify(waterLogRepository).save(updated);
        verify(nutritionSyncService).syncAfterWaterLog(USER_ID, LOGGED_AT);
    }

    @Test
    void updateOfAnotherUsersLogLooksLikeAMissingOne() {
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateWaterLog(LOG_ID, request("500"), OTHER_USER_ID));

        verify(waterLogRepository, never()).save(any());
        verify(nutritionSyncService, never()).syncAfterWaterLog(any(), any());
    }

    @Test
    void updateSucceedsEvenWhenTheSummarySyncFails() {
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));
        doThrow(new IllegalStateException("sync down")).when(nutritionSyncService).syncAfterWaterLog(USER_ID, LOGGED_AT);

        WaterLog updated = service.updateWaterLog(LOG_ID, request("500"), USER_ID);

        assertEquals(new BigDecimal("500"), updated.getAmountMl());
    }

    @Test
    void deleteRemovesTheLogAndSyncsTheLoggedDay() {
        WaterLog log = existingLog();
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(log));

        assertTrue(service.deleteWaterLog(LOG_ID, USER_ID));

        verify(waterLogRepository).delete(log);
        verify(nutritionSyncService).syncAfterWaterLog(USER_ID, LOGGED_AT);
    }

    @Test
    void deleteOfAnotherUsersLogDeletesNothing() {
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertFalse(service.deleteWaterLog(LOG_ID, OTHER_USER_ID));

        verify(waterLogRepository, never()).delete(any());
        verify(nutritionSyncService, never()).syncAfterWaterLog(any(), any());
    }

    @Test
    void deleteSucceedsEvenWhenTheSummarySyncFails() {
        when(waterLogRepository.findByIdAndUser_Id(LOG_ID, USER_ID)).thenReturn(Optional.of(existingLog()));
        doThrow(new IllegalStateException("sync down")).when(nutritionSyncService).syncAfterWaterLog(USER_ID, LOGGED_AT);

        assertTrue(service.deleteWaterLog(LOG_ID, USER_ID));
    }
}
