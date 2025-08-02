package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.VoiceChatSessionDTO;

public interface VoiceChatSessionService {
    /**
     * Create a new voice chat session
     * @param userId the user ID
     * @return the created session DTO
     */
    VoiceChatSessionDTO createSession(String userId);
    
    /**
     * Get an existing session
     * @param sessionId the session ID
     * @return the session DTO or null if not found
     */
    VoiceChatSessionDTO getSession(String sessionId);
    
    /**
     * Update session status
     * @param sessionId the session ID
     * @param status the new status
     * @return the updated session DTO
     */
    VoiceChatSessionDTO updateSessionStatus(String sessionId, String status);
    
    /**
     * End a session
     * @param sessionId the session ID
     * @return the ended session DTO
     */
    VoiceChatSessionDTO endSession(String sessionId);
    
    /**
     * Increment message count for a session
     * @param sessionId the session ID
     * @param messageType the type of message
     */
    void incrementMessageCount(String sessionId, String messageType);
    
    /**
     * Check if a session is active
     * @param sessionId the session ID
     * @return true if the session is active
     */
    boolean isSessionActive(String sessionId);
}