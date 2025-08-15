package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.CreateMedicationRequest;
import com.bytehealers.healverse.dto.request.LogMedicationRequest;
import com.bytehealers.healverse.dto.response.MedicationResponse;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.MedicationLogRepository;
import com.bytehealers.healverse.repo.MedicationRepository;
import com.bytehealers.healverse.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationLogRepository logRepository;
    private final UserRepository userRepository;

    public MedicationService(MedicationRepository medicationRepository,
                             MedicationLogRepository logRepository,
                             UserRepository userRepository) {
        this.medicationRepository = medicationRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    public MedicationResponse createMedication(Long userId, CreateMedicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Medication medication = new Medication();
        medication.setUser(user);
        medication.setName(request.getName());
        medication.setDosage(request.getDosage());
        medication.setType(request.getType());
        medication.setFrequency(request.getFrequency());
        medication.setStartDate(request.getStartDate());
        medication.setEndDate(request.getEndDate());
        medication.setNotes(request.getNotes());

        // Save medication first
        medication = medicationRepository.save(medication);

        // Create schedules
        List<MedicationSchedule> schedules = new ArrayList<>();
        for (String timeStr : request.getScheduleTimes()) {
            MedicationSchedule schedule = new MedicationSchedule();
            schedule.setMedication(medication);
            schedule.setTime(LocalTime.parse(timeStr));
            schedules.add(schedule);
        }

        medication.setSchedules(schedules);
        medication = medicationRepository.save(medication);

        // Generate initial logs for the next 30 days
        generateInitialLogs(medication);

        return MedicationResponse.from(medication);
    }

    public List<MedicationResponse> getUserMedications(Long userId) {
        List<Medication> medications = medicationRepository.findActiveByUserId(userId);
        return medications.stream()
                .map(MedicationResponse::from)
                .collect(Collectors.toList());
    }

    public MedicationResponse updateMedication(UUID medicationId, CreateMedicationRequest request) {
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException("Medication not found"));

        medication.setName(request.getName());
        medication.setDosage(request.getDosage());
        medication.setType(request.getType());
        medication.setFrequency(request.getFrequency());
        medication.setStartDate(request.getStartDate());
        medication.setEndDate(request.getEndDate());
        medication.setNotes(request.getNotes());

        // Update schedules
        medication.getSchedules().clear();
        for (String timeStr : request.getScheduleTimes()) {
            MedicationSchedule schedule = new MedicationSchedule();
            schedule.setMedication(medication);
            schedule.setTime(LocalTime.parse(timeStr));
            medication.getSchedules().add(schedule);
        }

        medication = medicationRepository.save(medication);
        return MedicationResponse.from(medication);
    }

    public void deleteMedication(UUID medicationId) {
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException("Medication not found"));

        medication.setIsActive(false);
        medicationRepository.save(medication);
    }

    public void logMedication(UUID medicationId, LogMedicationRequest request) {
        // scheduledTime is a LocalDateTime from the request
        LocalDateTime scheduledTime = request.getScheduledTime();
        // Define a 1-minute window for the log (adjust precision as needed)
        LocalDateTime end = scheduledTime.plusMinutes(1);

        MedicationLog log = logRepository.findTodayLog(medicationId, scheduledTime, end)
                .orElseThrow(() -> new RuntimeException("No scheduled log found for this time"));

        log.setStatus(request.getStatus());
        log.setActualTime(
                request.getActualTime() != null ? request.getActualTime() : LocalDateTime.now()
        );
        log.setNotes(request.getNotes());

        logRepository.save(log);
    }


    private void generateInitialLogs(Medication medication) {
        LocalDate startDate = medication.getStartDate();
        LocalDate endDate = medication.getEndDate();

        for (LocalDate date = startDate; date.isBefore(endDate) || date.equals(endDate); date = date.plusDays(1)) {
            if (shouldTakeMedicationOnDate(medication, date)) {
                for (MedicationSchedule schedule : medication.getSchedules()) {
                    LocalDateTime scheduledTime = date.atTime(schedule.getTime());

                    // Only create future logs
                    if (scheduledTime.isAfter(LocalDateTime.now())) {
                        MedicationLog log = new MedicationLog();
                        log.setMedication(medication);
                        log.setScheduledTime(scheduledTime);
                        log.setStatus(LogStatus.PENDING);
                        logRepository.save(log);
                    }
                }
            }
        }
    }

    private boolean shouldTakeMedicationOnDate(Medication medication, LocalDate date) {
        LocalDate startDate = medication.getStartDate();
        long daysDiff = ChronoUnit.DAYS.between(startDate, date);

        return switch (medication.getFrequency()) {
            case DAILY, TWICE_DAILY, THREE_TIMES -> true;
            case WEEKLY -> daysDiff % 7 == 0;
        };
    }
}