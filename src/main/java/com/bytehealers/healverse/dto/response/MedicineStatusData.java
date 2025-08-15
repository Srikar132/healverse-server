package com.bytehealers.healverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicineStatusData {
    private int taken;
    private int missed;
    private int skipped;
}