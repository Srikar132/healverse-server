package com.bytehealers.healverse.service.impl;

import com.bytehealers.healverse.dto.VoiceChatSessionDTO;
import com.bytehealers.healverse.service.VoiceChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VoiceChatSessionServiceImpl implements VoiceChatSessionService {

    // In-memory session storage (in production, this would be a database)
    private final Map<String, VoiceChatSessionDTO> sessions = new ConcurrentHashMap<>();

    @Override
    public VoiceChatSessionDTO createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        
        VoiceChatSessionDTO session = VoiceChatSessionDTO.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status("active")
                .startTime(currentTime)
                .messageCount(0)
                .build();
        
        sessions.put(sessionId, session);
        log.info("Created new voice chat session: {} for user: {}", sessionId, userId);
        
        return session;
    }

    @Override
    public VoiceChatSessionDTO getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public VoiceChatSessionDTO updateSessionStatus(String sessionId, String status) {
        VoiceChatSessionDTO session = sessions.get(sessionId);
        if (session != null) {
            session.setStatus(status);
            log.info("Updated session {} status to: {}", sessionId, status);
        }
        return session;
    }

    @Override
    public VoiceChatSessionDTO endSession(String sessionId) {
        VoiceChatSessionDTO session = sessions.get(sessionId);
        if (session != null) {
            session.setStatus("ended");
            session.setEndTime(System.currentTimeMillis());
            log.info("Ended voice chat session: {}", sessionId);
        }
        return session;
    }

    @Override
    public void incrementMessageCount(String sessionId, String messageType) {
        VoiceChatSessionDTO session = sessions.get(sessionId);
        if (session != null) {
            session.setMessageCount(session.getMessageCount() + 1);
            session.setLastMessageType(messageType);
            log.debug("Incremented message count for session {}: {} messages", sessionId, session.getMessageCount());
        }
    }

    @Override
    public boolean isSessionActive(String sessionId) {
        VoiceChatSessionDTO session = sessions.get(sessionId);
        return session != null && "active".equals(session.getStatus());
    }
}