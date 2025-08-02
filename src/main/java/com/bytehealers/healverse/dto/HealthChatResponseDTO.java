package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthChatResponseDTO {
    private String sessionId;
    private String responseText;
    private String audioResponse; // Base64 encoded audio
    private String audioFormat;
    private Boolean hasDisclaimer;
    private String disclaimerText;
    private Boolean isEmergency;
    private String emergencyMessage;
    private Long timestamp;
}