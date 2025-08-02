package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthChatResponseDTO {
    
    private String sessionId;
    private String response;
    private String responseType; // "general", "medical_advice", "emergency_alert", "information"
    private Boolean isEmergency;
    private String disclaimer;
    private List<String> suggestedQuestions;
    private String conversationSummary;
    private LocalDateTime timestamp;
    private String confidenceLevel; // "high", "medium", "low"
    
    // Emergency response fields
    private String emergencyContactInfo;
    private String emergencyInstructions;
    
    // Health-specific response metadata
    private List<String> healthTopics;
    private String medicalDisclaimer;
}