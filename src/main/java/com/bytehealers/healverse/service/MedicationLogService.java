package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.LogStatus;
import com.bytehealers.healverse.model.MedicationLog;
import com.bytehealers.healverse.repo.MedicationLogRepository;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicationLogService {

    private final MedicationLogRepository logRepository;

    public MedicationLogService(MedicationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // Every 15 minutes
    @Scheduled(cron = "0 0 * * * *")
    public void markMissedMedications() {
        LocalDateTime now = LocalDateTime.now();

        List<MedicationLog> overdueLogs = logRepository
                .findByStatusAndScheduledTimeBefore(LogStatus.PENDING, now);

        overdueLogs.forEach(log -> log.setStatus(LogStatus.MISSED));

        if (!overdueLogs.isEmpty()) {
            logRepository.saveAll(overdueLogs);
            System.out.println("Marked " + overdueLogs.size() + " logs as MISSED at " + now);
        }
    }
}
