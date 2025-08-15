package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.model.MedicationSchedule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private UUID id;
    private String time;
    private Boolean isActive;

    public static ScheduleResponse from(MedicationSchedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setTime(schedule.getTime().toString());
        response.setIsActive(schedule.getIsActive());
        return response;
    }
}