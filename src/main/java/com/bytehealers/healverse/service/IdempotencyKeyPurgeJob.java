package com.bytehealers.healverse.service;

import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Deletes idempotency records that can no longer be replayed. A client only reuses a key while
 * the request is still queued, so a week is far longer than any retry window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyPurgeJob {

    static final Duration RETENTION = Duration.ofDays(7);

    private final IdempotencyKeyRepository repository;

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void purge() {
        int deleted = repository.deleteCreatedBefore(LocalDateTime.now().minus(RETENTION));
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
