package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.model.IdempotencyKey;
import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes a write request safe to retry. A request that carries an {@code Idempotency-Key} runs
 * once per (user, key): its response is stored, and any retry with the same key gets that
 * stored response back instead of running the action again.
 *
 * Without a key the action simply runs, so existing clients are unaffected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    static final int MAX_KEY_LENGTH = 64;

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public <T> T execute(Long userId, String key, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
        }

        Optional<T> replay = findStored(userId, key, responseType);
        if (replay.isPresent()) {
            return replay.get();
        }

        T result = action.get();
        store(userId, key, result);
        return result;
    }

    private <T> Optional<T> findStored(Long userId, String key, Class<T> responseType) {
        return repository.findByUserIdAndKey(userId, key).map(stored -> {
            try {
                return objectMapper.readValue(stored.getResponseJson(), responseType);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Stored response for idempotency key could not be read", e);
            }
        });
    }

    // Recording is best effort: the action already succeeded, so a failure here must not turn
    // the request into an error (the client would retry and create the duplicate we prevent).
    private void store(Long userId, String key, Object result) {
        try {
            IdempotencyKey entry = new IdempotencyKey();
            entry.setUserId(userId);
            entry.setKey(key);
            entry.setResponseJson(objectMapper.writeValueAsString(result));
            repository.save(entry);
        } catch (JsonProcessingException | DataIntegrityViolationException e) {
            log.warn("Could not record idempotency key for user {}: {}", userId, e.getMessage());
        }
    }
}
