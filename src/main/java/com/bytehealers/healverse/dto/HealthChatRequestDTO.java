package com.bytehealers.healverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HealthChatRequestDTO {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Query is required")
    private String query;
    
    private String conversationContext;
    
    private String healthTopicCategory; // e.g., "general", "symptoms", "medication", "mental_health", "emergency"
}