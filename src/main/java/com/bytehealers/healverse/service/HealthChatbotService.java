package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.HealthChatResponseDTO;

public interface HealthChatbotService {
    /**
     * Process a health-related query and generate a response
     * @param sessionId the chat session ID
     * @param userId the user ID
     * @param userMessage the user's message/query
     * @return the chatbot response with safety measures
     */
    HealthChatResponseDTO processHealthQuery(String sessionId, String userId, String userMessage);
    
    /**
     * Check if the message contains emergency keywords
     * @param message the user message
     * @return true if emergency keywords are detected
     */
    boolean detectEmergency(String message);
    
    /**
     * Get medical disclaimer text
     * @return the disclaimer text
     */
    String getMedicalDisclaimer();
}