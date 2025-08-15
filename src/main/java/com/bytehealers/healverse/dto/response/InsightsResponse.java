package com.bytehealers.healverse.dto.response;

import com.bytehealers.healverse.dto.internal.InsightItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsightsResponse {
    private List<InsightItem> medicationInsights;
    private List<InsightItem> dietInsights;
    private List<InsightItem> healthInsights;

}