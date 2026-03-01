package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.*;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.MedicationLogRepository;
import com.bytehealers.healverse.repo.MedicationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MedicationAnalyticsService {

    private final MedicationLogRepository logRepository;
    private final MedicationRepository medicationRepository;

    public MedicationAnalyticsService(MedicationLogRepository logRepository,
                            MedicationRepository medicationRepository) {
        this.logRepository = logRepository;
        this.medicationRepository = medicationRepository;
    }

    public DashboardStatsResponse getDashboardStats(Long userId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Existing logic
        List<MedicationLog> todayLogs = logRepository.findByUserIdAndDate(userId, LocalDate.now());
        int todayTotal = todayLogs.size();
        int todayTaken = (int) todayLogs.stream().filter(log -> log.getStatus() == LogStatus.TAKEN).count();

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        List<MedicationLog> weekLogs = logRepository.findByUserIdAndDateRange(
                userId, weekAgo, LocalDateTime.now()
        );
        int adherenceRate = calculateAdherenceRate(weekLogs);
        int currentStreak = calculateCurrentStreak(userId);

        List<Medication> activeMedications = medicationRepository.findActiveByUserId(userId);
        List<TodayMedicationResponse> todayMedications = getTodayMedications(userId);

        // Set existing data
        stats.setAdherenceRate(adherenceRate);
        stats.setCurrentStreak(currentStreak);
        stats.setTotalMedications(activeMedications.size());
        stats.setTodayTaken(todayTaken);
        stats.setTodayTotal(todayTotal);
        stats.setTodayMedications(todayMedications);

        // New analytics data
        stats.setWeeklyAdherence(getWeeklyAdherenceData(userId));
        stats.setDailyIntake(getDailyIntakeData(userId));
        stats.setMedicineStatus(getMedicineStatusData(userId));

        return stats;
    }

    private WeeklyAdherenceData getWeeklyAdherenceData(Long userId) {
        WeeklyAdherenceData weeklyData = new WeeklyAdherenceData();
        List<String> labels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Integer> adherenceData = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            List<MedicationLog> dayLogs = logRepository.findByUserIdAndDate(userId, date);

            if (dayLogs.isEmpty()) {
                adherenceData.add(0);
            } else {
                // Only count completed logs for accurate adherence calculation
                List<MedicationLog> completedLogs = dayLogs.stream()
                        .filter(log -> log.getStatus() != LogStatus.PENDING)
                        .collect(Collectors.toList());
                        
                if (completedLogs.isEmpty()) {
                    adherenceData.add(0); // All logs still pending
                } else {
                    long takenCount = completedLogs.stream()
                            .filter(log -> log.getStatus() == LogStatus.TAKEN)
                            .count();
                    int adherencePercent = (int) ((takenCount * 100) / completedLogs.size());
                    adherenceData.add(adherencePercent);
                }
            }
        }

        weeklyData.setLabels(labels);
        weeklyData.setData(adherenceData);
        return weeklyData;
    }

    private List<DailyIntakeData> getDailyIntakeData(Long userId) {
        List<DailyIntakeData> dailyIntake = new ArrayList<>();
        List<String> dayLabels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            List<MedicationLog> dayLogs = logRepository.findByUserIdAndDate(userId, date);

            int intakePercent = 0;
            if (!dayLogs.isEmpty()) {
                // Only count completed logs for accurate intake calculation
                List<MedicationLog> completedLogs = dayLogs.stream()
                        .filter(log -> log.getStatus() != LogStatus.PENDING)
                        .collect(Collectors.toList());
                        
                if (!completedLogs.isEmpty()) {
                    long takenCount = completedLogs.stream()
                            .filter(log -> log.getStatus() == LogStatus.TAKEN)
                            .count();
                    intakePercent = (int) ((takenCount * 100) / completedLogs.size());
                }
            }

            DailyIntakeData dailyData = new DailyIntakeData();
            dailyData.setDay(dayLabels.get(i));
            dailyData.setValue(intakePercent);
            dailyIntake.add(dailyData);
        }

        return dailyIntake;
    }

    private MedicineStatusData getMedicineStatusData(Long userId) {
        // Get last 7 days data for better statistics
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        List<MedicationLog> weekLogs = logRepository.findByUserIdAndDateRange(
                userId, weekAgo, LocalDateTime.now()
        );

        if (weekLogs.isEmpty()) {
            return new MedicineStatusData(0, 0, 0);
        }

        long takenCount = weekLogs.stream().filter(log -> log.getStatus() == LogStatus.TAKEN).count();
        long missedCount = weekLogs.stream().filter(log -> log.getStatus() == LogStatus.MISSED).count();
        long skippedCount = weekLogs.stream().filter(log -> log.getStatus() == LogStatus.SKIPPED).count();

        int total = weekLogs.size();
        int takenPercent = (int) ((takenCount * 100) / total);
        int missedPercent = (int) ((missedCount * 100) / total);
        int skippedPercent = (int) ((skippedCount * 100) / total);

        MedicineStatusData statusData = new MedicineStatusData();
        statusData.setTaken(takenPercent);
        statusData.setMissed(missedPercent);
        statusData.setSkipped(skippedPercent);

        return statusData;
    }

    public List<TodayMedicationResponse> getTodayMedications(Long userId) {
        List<MedicationLog> todayLogs = logRepository.findByUserIdAndDate(userId, LocalDate.now());

        return todayLogs.stream()
                .map(log -> {
                    TodayMedicationResponse response = new TodayMedicationResponse();
                    response.setId(log.getId());
                    response.setMedicationId(log.getMedication().getId());
                    response.setName(log.getMedication().getName());
                    response.setDosage(log.getMedication().getDosage());
                    response.setType(log.getMedication().getType());
                    response.setScheduledTime(log.getScheduledTime());
                    response.setStatus(log.getStatus());
                    response.setActualTime(log.getActualTime());
                    return response;
                })
                .sorted(Comparator.comparing(TodayMedicationResponse::getScheduledTime))
                .collect(Collectors.toList());
    }

    private int calculateAdherenceRate(List<MedicationLog> logs) {
        if (logs.isEmpty()) {
            return 0; // No medications = 0% adherence, not 100%
        }

        // Count only completed logs (TAKEN, MISSED, SKIPPED) - exclude PENDING
        List<MedicationLog> completedLogs = logs.stream()
                .filter(log -> log.getStatus() != LogStatus.PENDING)
                .collect(Collectors.toList());
                
        if (completedLogs.isEmpty()) {
            return 0; // All logs are still pending
        }

        long takenCount = completedLogs.stream()
                .filter(log -> log.getStatus() == LogStatus.TAKEN)
                .count();
                
        return (int) Math.round((double) takenCount / completedLogs.size() * 100);
    }

    private int calculateCurrentStreak(Long userId) {
        // Get logs from the last 30 days, ordered by date desc
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<MedicationLog> recentLogs = logRepository.findByUserIdAndDateRange(
                userId, thirtyDaysAgo, LocalDateTime.now()
        );

        // Group by date and check daily adherence
        Map<LocalDate, List<MedicationLog>> logsByDate = recentLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getScheduledTime().toLocalDate()));

        int streak = 0;
        LocalDate currentDate = LocalDate.now();

        while (logsByDate.containsKey(currentDate)) {
            List<MedicationLog> dayLogs = logsByDate.get(currentDate);
            
            // Filter out pending logs for fair streak calculation
            List<MedicationLog> completedLogs = dayLogs.stream()
                    .filter(log -> log.getStatus() != LogStatus.PENDING)
                    .collect(Collectors.toList());
                    
            if (completedLogs.isEmpty()) {
                // Skip days with no completed logs (all pending)
                currentDate = currentDate.minusDays(1);
                continue;
            }
            
            long takenCount = completedLogs.stream()
                    .filter(log -> log.getStatus() == LogStatus.TAKEN)
                    .count();
                    
            // More lenient streak calculation: 80% adherence maintains streak
            double adherenceRate = (double) takenCount / completedLogs.size();
            
            if (adherenceRate >= 0.80) { // 80% threshold instead of 100%
                streak++;
                currentDate = currentDate.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }
}