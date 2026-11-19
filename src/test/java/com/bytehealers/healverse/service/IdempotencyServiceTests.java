package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.exception.TooManyRequestsException;
import com.bytehealers.healverse.model.IdempotencyKey;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceTests {

    private static final Long USER_ID = 7L;

    private IdempotencyKeyRepository repository;
    private ObjectMapper objectMapper;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyKeyRepository.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new IdempotencyService(repository, objectMapper, Duration.ofSeconds(5), Duration.ofMinutes(2));
    }

    private static WaterLog waterLog(long id) {
        WaterLog log = new WaterLog();
        log.setId(id);
        log.setAmountMl(new BigDecimal("250.00"));
        log.setLoggedAt(LocalDateTime.of(2026, 1, 2, 10, 30));
        return log;
    }

    private IdempotencyKey claimed(WaterLog response, LocalDateTime createdAt) throws Exception {
        IdempotencyKey entry = new IdempotencyKey();
        entry.setUserId(USER_ID);
        entry.setKey("k1");
        entry.setCreatedAt(createdAt);
        if (response != null) {
            entry.setResponseJson(objectMapper.writeValueAsString(response));
        }
        return entry;
    }

    private void keyAlreadyClaimed() {
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
    }

    @Test
    void withoutKeyTheActionRunsEveryTimeAndNothingIsStored() {
        AtomicInteger calls = new AtomicInteger();

        service.execute(USER_ID, null, WaterLog.class, () -> waterLog(calls.incrementAndGet()));
        service.execute(USER_ID, " ", WaterLog.class, () -> waterLog(calls.incrementAndGet()));

        assertEquals(2, calls.get());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void firstRequestClaimsTheKeyRunsTheActionAndStoresItsResponse() {
        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> waterLog(1));

        assertEquals(1L, result.getId());
        verify(repository).saveAndFlush(any(IdempotencyKey.class));
        verify(repository).storeResponse(any(), anyString(), anyString());
    }

    @Test
    void retryWithSameKeyReturnsTheStoredResponseWithoutRunningTheAction() throws Exception {
        keyAlreadyClaimed();
        when(repository.findByUserIdAndKey(USER_ID, "k1"))
                .thenReturn(Optional.of(claimed(waterLog(42), LocalDateTime.now())));
        AtomicInteger calls = new AtomicInteger();

        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> {
            calls.incrementAndGet();
            return waterLog(99);
        });

        assertEquals(42L, result.getId());
        assertEquals(new BigDecimal("250.00"), result.getAmountMl());
        assertEquals(0, calls.get());
        verify(repository, never()).storeResponse(any(), anyString(), anyString());
    }

    @Test
    void failedActionReleasesTheKeySoTheClientCanRetry() {
        assertThrows(IllegalStateException.class, () -> service.execute(USER_ID, "k1", WaterLog.class, () -> {
            throw new IllegalStateException("boom");
        }));

        verify(repository).release(USER_ID, "k1");
        verify(repository, never()).storeResponse(any(), anyString(), anyString());
    }

    @Test
    void requestArrivingWhileTheFirstIsRunningWaitsForItsResponse() throws Exception {
        keyAlreadyClaimed();
        when(repository.findByUserIdAndKey(USER_ID, "k1")).thenReturn(
                Optional.of(claimed(null, LocalDateTime.now())),
                Optional.of(claimed(waterLog(42), LocalDateTime.now())));
        AtomicInteger calls = new AtomicInteger();

        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> waterLog(calls.incrementAndGet()));

        assertEquals(42L, result.getId());
        assertEquals(0, calls.get());
    }

    @Test
    void requestStillRunningAfterTheWaitIsAskedToRetry() throws Exception {
        IdempotencyService impatient = new IdempotencyService(repository, objectMapper, Duration.ZERO, Duration.ofMinutes(2));
        keyAlreadyClaimed();
        when(repository.findByUserIdAndKey(USER_ID, "k1")).thenReturn(Optional.of(claimed(null, LocalDateTime.now())));

        assertThrows(TooManyRequestsException.class,
                () -> impatient.execute(USER_ID, "k1", WaterLog.class, () -> waterLog(1)));
    }

    @Test
    void claimLeftByACrashedRequestIsTakenOver() throws Exception {
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByUserIdAndKey(USER_ID, "k1"))
                .thenReturn(Optional.of(claimed(null, LocalDateTime.now().minusMinutes(10))));

        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> waterLog(5));

        assertEquals(5L, result.getId());
        verify(repository).release(USER_ID, "k1");
    }

    @Test
    void keyLongerThanTheColumnIsRejected() {
        String tooLong = "x".repeat(IdempotencyService.MAX_KEY_LENGTH + 1);

        assertThrows(BadRequestException.class,
                () -> service.execute(USER_ID, tooLong, WaterLog.class, () -> waterLog(1)));
    }
}
