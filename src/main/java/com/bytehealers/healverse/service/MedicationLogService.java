package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.LogStatus;
import com.bytehealers.healverse.model.MedicationLog;
import com.bytehealers.healverse.repo.MedicationLogRepository;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MedicationLogService {

    private final MedicationLogRepository logRepository;

    public MedicationLogService(MedicationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // Every 15 minutes - check for missed medications
    @Scheduled(cron = "0 */15 * * * *")
    public void markMissedMedications() {
        LocalDateTime now = LocalDateTime.now();
        
        // Grace period: Mark as missed only if 4 hours past scheduled time
        // This aligns with the logging window in MedicationService
        LocalDateTime cutoffTime = now.minusHours(4);

        List<MedicationLog> overdueLogs = logRepository
                .findByStatusAndScheduledTimeBefore(LogStatus.PENDING, cutoffTime);

        if (!overdueLogs.isEmpty()) {
            overdueLogs.forEach(log -> {
                log.setStatus(LogStatus.MISSED);
                // Add system note about when it was marked as missed
                String systemNote = String.format("Automatically marked as missed at %s (%.1f hours after scheduled time)", 
                                                now.toString(), 
                                                ChronoUnit.MINUTES.between(log.getScheduledTime(), now) / 60.0);
                log.setNotes(log.getNotes() != null ? log.getNotes() + " | " + systemNote : systemNote);
            });
            
            logRepository.saveAll(overdueLogs);
            System.out.println("Marked " + overdueLogs.size() + " medications as MISSED at " + now + 
                             " (cutoff: " + cutoffTime + ")");
        }
    }
}
