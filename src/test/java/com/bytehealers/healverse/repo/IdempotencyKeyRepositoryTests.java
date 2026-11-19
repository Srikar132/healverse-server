package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.IdempotencyKey;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Each repository call commits on its own, as in production, so the test itself is not transactional.
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdempotencyKeyRepositoryTests {

    private static final Long USER_ID = 7L;

    @Autowired
    private IdempotencyKeyRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private static IdempotencyKey claim(String key, LocalDateTime createdAt) {
        IdempotencyKey entry = new IdempotencyKey();
        entry.setUserId(USER_ID);
        entry.setKey(key);
        entry.setCreatedAt(createdAt);
        return entry;
    }

    @Test
    void sameUserAndKeyCannotBeClaimedTwiceButOtherUsersAndKeysCan() {
        repository.saveAndFlush(claim("k1", LocalDateTime.now()));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(claim("k1", LocalDateTime.now())));

        IdempotencyKey otherUser = claim("k1", LocalDateTime.now());
        otherUser.setUserId(8L);
        repository.saveAndFlush(otherUser);
        repository.saveAndFlush(claim("k2", LocalDateTime.now()));
        assertEquals(3, repository.count());
    }

    @Test
    void responseIsStoredOnTheClaimedRowAndReleaseRemovesIt() {
        repository.saveAndFlush(claim("k1", LocalDateTime.now()));
        assertNull(repository.findByUserIdAndKey(USER_ID, "k1").orElseThrow().getResponseJson());

        repository.storeResponse(USER_ID, "k1", "{\"id\":1}");
        assertEquals("{\"id\":1}", repository.findByUserIdAndKey(USER_ID, "k1").orElseThrow().getResponseJson());

        repository.release(USER_ID, "k1");
        assertTrue(repository.findByUserIdAndKey(USER_ID, "k1").isEmpty());
    }

    @Test
    void purgeDeletesOnlyRowsOlderThanTheCutoff() {
        repository.saveAndFlush(claim("old", LocalDateTime.now().minusDays(8)));
        repository.saveAndFlush(claim("recent", LocalDateTime.now().minusDays(1)));

        int deleted = repository.deleteCreatedBefore(LocalDateTime.now().minusDays(7));

        assertEquals(1, deleted);
        assertTrue(repository.findByUserIdAndKey(USER_ID, "old").isEmpty());
        assertTrue(repository.findByUserIdAndKey(USER_ID, "recent").isPresent());
    }

    @Test
    void simultaneousRequestsWithTheSameKeyRunTheActionOnce() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        IdempotencyService service = new IdempotencyService(repository, objectMapper);
        int requests = 8;
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);

        try {
            List<Future<WaterLog>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.execute(USER_ID, "same-key", WaterLog.class, () -> {
                        sleep(300);
                        WaterLog log = new WaterLog();
                        log.setId((long) executions.incrementAndGet());
                        log.setAmountMl(new BigDecimal("250.00"));
                        log.setLoggedAt(LocalDateTime.of(2026, 1, 2, 10, 30));
                        return log;
                    });
                }));
            }
            start.countDown();

            for (Future<WaterLog> result : results) {
                assertEquals(1L, result.get(20, TimeUnit.SECONDS).getId());
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, executions.get());
        assertEquals(1, repository.count());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
