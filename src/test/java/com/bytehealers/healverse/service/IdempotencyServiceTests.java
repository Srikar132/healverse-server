package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.model.IdempotencyKey;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        service = new IdempotencyService(repository, objectMapper);
    }

    private static WaterLog waterLog(long id) {
        WaterLog log = new WaterLog();
        log.setId(id);
        log.setAmountMl(new BigDecimal("250.00"));
        log.setLoggedAt(LocalDateTime.of(2026, 1, 2, 10, 30));
        return log;
    }

    @Test
    void withoutKeyTheActionRunsEveryTimeAndNothingIsStored() {
        AtomicInteger calls = new AtomicInteger();

        service.execute(USER_ID, null, WaterLog.class, () -> waterLog(calls.incrementAndGet()));
        service.execute(USER_ID, " ", WaterLog.class, () -> waterLog(calls.incrementAndGet()));

        assertEquals(2, calls.get());
        verify(repository, never()).save(any());
    }

    @Test
    void firstRequestWithKeyRunsTheActionAndStoresItsResponse() {
        when(repository.findByUserIdAndKey(USER_ID, "k1")).thenReturn(Optional.empty());

        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> waterLog(1));

        assertEquals(1L, result.getId());
        verify(repository).save(any(IdempotencyKey.class));
    }

    @Test
    void retryWithSameKeyReturnsTheStoredResponseWithoutRunningTheAction() throws Exception {
        IdempotencyKey stored = new IdempotencyKey();
        stored.setResponseJson(objectMapper.writeValueAsString(waterLog(42)));
        when(repository.findByUserIdAndKey(USER_ID, "k1")).thenReturn(Optional.of(stored));
        AtomicInteger calls = new AtomicInteger();

        WaterLog result = service.execute(USER_ID, "k1", WaterLog.class, () -> {
            calls.incrementAndGet();
            return waterLog(99);
        });

        assertEquals(42L, result.getId());
        assertEquals(new BigDecimal("250.00"), result.getAmountMl());
        assertEquals(0, calls.get());
        verify(repository, never()).save(any());
    }

    @Test
    void keyLongerThanTheColumnIsRejected() {
        String tooLong = "x".repeat(IdempotencyService.MAX_KEY_LENGTH + 1);

        assertThrows(BadRequestException.class,
                () -> service.execute(USER_ID, tooLong, WaterLog.class, () -> waterLog(1)));
    }
}
