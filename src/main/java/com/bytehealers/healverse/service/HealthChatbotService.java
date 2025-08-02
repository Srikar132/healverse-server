package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;

public interface HealthChatbotService {
    
    /**
     * Process a health-related query using Spring AI
     * @param request The health chat request containing query and context
     * @return Health chat response with AI-generated content and metadata
     */
    HealthChatResponseDTO processHealthQuery(HealthChatRequestDTO request);
    
    /**
     * Start a new conversation session
     * @param userId User ID for the session
     * @return Session ID for the new conversation
     */
    String startNewSession(Long userId);
    
    /**
     * End a conversation session and optionally get summary
     * @param sessionId Session ID to end
     * @return Conversation summary if requested
     */
    String endSession(String sessionId);
    
    /**
     * Check if a query indicates a potential emergency
     * @param query The user's query
     * @return true if emergency keywords are detected
     */
    boolean detectEmergency(String query);
}