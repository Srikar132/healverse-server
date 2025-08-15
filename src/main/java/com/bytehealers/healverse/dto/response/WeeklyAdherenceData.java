package com.bytehealers.healverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAdherenceData {
    private List<String> labels;
    private List<Integer> data;
}