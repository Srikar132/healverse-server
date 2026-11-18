package com.bytehealers.healverse.service;

import com.bytehealers.healverse.repo.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IdempotencyKeyPurgeJobTests {

    @Test
    void purgesRowsOlderThanTheRetentionPeriod() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        LocalDateTime before = LocalDateTime.now();

        new IdempotencyKeyPurgeJob(repository).purge();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteCreatedBefore(cutoff.capture());
        Duration age = Duration.between(cutoff.getValue(), before);
        assertTrue(age.compareTo(IdempotencyKeyPurgeJob.RETENTION.minusSeconds(5)) >= 0
                && age.compareTo(IdempotencyKeyPurgeJob.RETENTION.plusSeconds(5)) <= 0);
    }
}
