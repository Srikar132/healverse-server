package com.bytehealers.healverse.websocket;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.dto.VoiceChatMessageDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceChatWebSocketHandler implements WebSocketHandler {
    
    private final HealthChatbotService healthChatbotService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store active WebSocket sessions
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionChatMap = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.put(session.getId(), session);
        
        // Send welcome message
        VoiceChatMessageDTO welcomeMessage = VoiceChatMessageDTO.builder()
            .messageType("ai_response")
            .content("Hello! I'm your health assistant. How can I help you today?")
            .timestamp(LocalDateTime.now())
            .isProcessed(true)
            .processingStatus("completed")
            .build();
        
        sendMessage(session, welcomeMessage);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            log.info("Received WebSocket message: {}", payload);
            
            VoiceChatMessageDTO voiceMessage = objectMapper.readValue(payload, VoiceChatMessageDTO.class);
            
            if ("user_text".equals(voiceMessage.getMessageType())) {
                handleUserTextMessage(session, voiceMessage);
            } else if ("voice_start".equals(voiceMessage.getMessageType())) {
                handleVoiceStart(session, voiceMessage);
            } else if ("voice_end".equals(voiceMessage.getMessageType())) {
                handleVoiceEnd(session, voiceMessage);
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket message: ", e);
            sendErrorMessage(session, "Error processing your message. Please try again.");
        }
    }
    
    private void handleUserTextMessage(WebSocketSession session, VoiceChatMessageDTO voiceMessage) {
        try {
            // Get or create chat session
            String chatSessionId = sessionChatMap.computeIfAbsent(
                session.getId(), 
                k -> healthChatbotService.startNewSession(voiceMessage.getUserId())
            );
            
            // Create health chat request
            HealthChatRequestDTO healthRequest = new HealthChatRequestDTO();
            healthRequest.setSessionId(chatSessionId);
            healthRequest.setUserId(voiceMessage.getUserId());
            healthRequest.setQuery(voiceMessage.getContent());
            healthRequest.setHealthTopicCategory("general");
            
            // Process with AI
            HealthChatResponseDTO aiResponse = healthChatbotService.processHealthQuery(healthRequest);
            
            // Convert to voice message
            VoiceChatMessageDTO responseMessage = VoiceChatMessageDTO.builder()
                .sessionId(chatSessionId)
                .messageType("ai_response")
                .content(aiResponse.getResponse())
                .timestamp(LocalDateTime.now())
                .isProcessed(true)
                .processingStatus("completed")
                .build();
            
            // Send response back
            sendMessage(session, responseMessage);
            
            // If emergency, send additional alert
            if (aiResponse.getIsEmergency()) {
                VoiceChatMessageDTO emergencyAlert = VoiceChatMessageDTO.builder()
                    .sessionId(chatSessionId)
                    .messageType("emergency_alert")
                    .content("EMERGENCY DETECTED: " + aiResponse.getEmergencyInstructions())
                    .timestamp(LocalDateTime.now())
                    .isProcessed(true)
                    .processingStatus("completed")
                    .build();
                
                sendMessage(session, emergencyAlert);
            }
            
        } catch (Exception e) {
            log.error("Error processing user text message: ", e);
            sendErrorMessage(session, "Sorry, I couldn't process your message. Please try again.");
        }
    }
    
    private void handleVoiceStart(WebSocketSession session, VoiceChatMessageDTO voiceMessage) {
        // Handle voice recording start
        log.info("Voice recording started for session: {}", session.getId());
        
        VoiceChatMessageDTO statusMessage = VoiceChatMessageDTO.builder()
            .messageType("status")
            .content("Listening...")
            .timestamp(LocalDateTime.now())
            .processingStatus("recording")
            .build();
        
        sendMessage(session, statusMessage);
    }
    
    private void handleVoiceEnd(WebSocketSession session, VoiceChatMessageDTO voiceMessage) {
        // Handle voice recording end and processing
        log.info("Voice recording ended for session: {}", session.getId());
        
        // In a full implementation, this would:
        // 1. Process the audio data with speech-to-text
        // 2. Convert to text message
        // 3. Process with health chatbot
        // 4. Convert response to speech
        
        VoiceChatMessageDTO statusMessage = VoiceChatMessageDTO.builder()
            .messageType("status")
            .content("Processing your voice message...")
            .timestamp(LocalDateTime.now())
            .processingStatus("processing")
            .build();
        
        sendMessage(session, statusMessage);
        
        // For now, send a placeholder response
        VoiceChatMessageDTO responseMessage = VoiceChatMessageDTO.builder()
            .messageType("ai_response")
            .content("I received your voice message. For now, please use text input. Voice processing will be available soon!")
            .timestamp(LocalDateTime.now())
            .isProcessed(true)
            .processingStatus("completed")
            .build();
        
        sendMessage(session, responseMessage);
    }
    
    private void sendMessage(WebSocketSession session, VoiceChatMessageDTO message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        } catch (IOException e) {
            log.error("Error sending WebSocket message: ", e);
        }
    }
    
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        VoiceChatMessageDTO errorMsg = VoiceChatMessageDTO.builder()
            .messageType("error")
            .content(errorMessage)
            .timestamp(LocalDateTime.now())
            .processingStatus("error")
            .build();
        
        sendMessage(session, errorMsg);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: ", session.getId(), exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), closeStatus);
        
        // Clean up session
        sessions.remove(session.getId());
        String chatSessionId = sessionChatMap.remove(session.getId());
        
        if (chatSessionId != null) {
            healthChatbotService.endSession(chatSessionId);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}