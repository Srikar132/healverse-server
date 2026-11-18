package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.exception.TooManyRequestsException;
import com.bytehealers.healverse.model.IdempotencyKey;
import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes a write request safe to retry. A request that carries an {@code Idempotency-Key} runs
 * once per (user, key): the key is claimed with a unique insert before the action runs, and the
 * response is stored when it succeeds. A retry with the same key gets that stored response back
 * instead of running the action again; a request that arrives while the first is still running
 * waits for it.
 *
 * If the action fails the claim is released, so the client can retry. Without a key the action
 * simply runs, so existing clients are unaffected.
 */
@Service
@Slf4j
public class IdempotencyService {

    static final int MAX_KEY_LENGTH = 64;

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_ABANDONED_AFTER = Duration.ofMinutes(2);
    private static final long POLL_INTERVAL_MS = 100;

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final Duration maxWait;
    private final Duration abandonedAfter;

    @Autowired
    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, DEFAULT_WAIT, DEFAULT_ABANDONED_AFTER);
    }

    IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper,
                       Duration maxWait, Duration abandonedAfter) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.maxWait = maxWait;
        this.abandonedAfter = abandonedAfter;
    }

    public <T> T execute(Long userId, String key, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
        }

        long deadline = System.nanoTime() + maxWait.toNanos();
        while (true) {
            if (claim(userId, key)) {
                return runClaimed(userId, key, action);
            }
            Optional<IdempotencyKey> existing = repository.findByUserIdAndKey(userId, key);
            if (existing.isEmpty()) {
                continue; // released by a failed first attempt: try to claim again
            }
            IdempotencyKey stored = existing.get();
            if (stored.getResponseJson() != null) {
                return read(stored.getResponseJson(), responseType);
            }
            if (stored.getCreatedAt().isBefore(LocalDateTime.now().minus(abandonedAfter))) {
                repository.release(userId, key); // the claiming request never finished (e.g. a crash)
                continue;
            }
            if (System.nanoTime() >= deadline) {
                throw new TooManyRequestsException("A request with this key is still being processed. Please retry shortly.");
            }
            pause();
        }
    }

    private boolean claim(Long userId, String key) {
        IdempotencyKey entry = new IdempotencyKey();
        entry.setUserId(userId);
        entry.setKey(key);
        try {
            repository.saveAndFlush(entry);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private <T> T runClaimed(Long userId, String key, Supplier<T> action) {
        T result;
        try {
            result = action.get();
        } catch (RuntimeException | Error e) {
            repository.release(userId, key);
            throw e;
        }
        store(userId, key, result);
        return result;
    }

    // Recording is best effort: the action already succeeded, so a failure here must not turn
    // the request into an error (the client would retry and create the duplicate we prevent).
    private void store(Long userId, String key, Object result) {
        try {
            repository.storeResponse(userId, key, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.warn("Could not record idempotency key for user {}: {}", userId, e.getMessage());
            repository.release(userId, key);
        }
    }

    private <T> T read(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored response for idempotency key could not be read", e);
        }
    }

    private static void pause() {
        try {
            Thread.sleep(POLL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a request with the same idempotency key", e);
        }
    }
}
