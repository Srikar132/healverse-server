package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.dto.VoiceMessageDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import com.bytehealers.healverse.service.SpeechToTextService;
import com.bytehealers.healverse.service.VoiceChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VoiceWebSocketController {

    private final SpeechToTextService speechToTextService;
    private final HealthChatbotService healthChatbotService;
    private final VoiceChatSessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/voice.message")
    public void handleVoiceMessage(@Payload VoiceMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received voice message: type={}, sessionId={}, userId={}", 
                 message.getMessageType(), message.getSessionId(), message.getUserId());

        try {
            // Validate session
            if (!sessionService.isSessionActive(message.getSessionId())) {
                log.warn("Inactive session: {}", message.getSessionId());
                sendErrorMessage(message.getSessionId(), "Session is not active");
                return;
            }

            // Process based on message type
            switch (message.getMessageType()) {
                case "audio":
                    handleAudioMessage(message);
                    break;
                case "text":
                    handleTextMessage(message);
                    break;
                case "control":
                    handleControlMessage(message);
                    break;
                default:
                    log.warn("Unknown message type: {}", message.getMessageType());
                    sendErrorMessage(message.getSessionId(), "Unknown message type: " + message.getMessageType());
            }

            // Update session statistics
            sessionService.incrementMessageCount(message.getSessionId(), message.getMessageType());

        } catch (Exception e) {
            log.error("Error processing voice message: {}", e.getMessage(), e);
            sendErrorMessage(message.getSessionId(), "Error processing message: " + e.getMessage());
        }
    }

    private void handleAudioMessage(VoiceMessageDTO message) {
        log.info("Processing audio message for session: {}", message.getSessionId());
        
        // Convert speech to text
        String transcribedText = speechToTextService.convertSpeechToText(
                message.getContent(), message.getAudioFormat());
        
        // Send transcription confirmation to user
        VoiceMessageDTO transcriptionMessage = VoiceMessageDTO.builder()
                .sessionId(message.getSessionId())
                .messageType("transcription")
                .content(transcribedText)
                .timestamp(System.currentTimeMillis())
                .isFromBot(true)
                .build();
        
        messagingTemplate.convertAndSendToUser(
                message.getUserId(), "/queue/voice", transcriptionMessage);
        
        // Process the transcribed text as a health query
        processHealthQuery(message.getSessionId(), message.getUserId(), transcribedText);
    }

    private void handleTextMessage(VoiceMessageDTO message) {
        log.info("Processing text message for session: {}", message.getSessionId());
        processHealthQuery(message.getSessionId(), message.getUserId(), message.getContent());
    }

    private void handleControlMessage(VoiceMessageDTO message) {
        log.info("Processing control message: {} for session: {}", message.getContent(), message.getSessionId());
        
        switch (message.getContent()) {
            case "start_session":
                // Session should already be active, but confirm
                log.info("Session start confirmed: {}", message.getSessionId());
                break;
            case "pause_session":
                sessionService.updateSessionStatus(message.getSessionId(), "paused");
                break;
            case "resume_session":
                sessionService.updateSessionStatus(message.getSessionId(), "active");
                break;
            case "end_session":
                sessionService.endSession(message.getSessionId());
                break;
            default:
                log.warn("Unknown control command: {}", message.getContent());
        }
    }

    private void processHealthQuery(String sessionId, String userId, String query) {
        // Get response from health chatbot
        HealthChatResponseDTO response = healthChatbotService.processHealthQuery(sessionId, userId, query);
        
        // Convert to voice message format
        VoiceMessageDTO responseMessage = VoiceMessageDTO.builder()
                .sessionId(sessionId)
                .messageType("response")
                .content(response.getResponseText())
                .audioFormat(response.getAudioFormat())
                .timestamp(response.getTimestamp())
                .isFromBot(true)
                .build();
        
        // Send response to user
        messagingTemplate.convertAndSendToUser(userId, "/queue/voice", responseMessage);
        
        // If there's audio response, send it separately
        if (response.getAudioResponse() != null) {
            VoiceMessageDTO audioResponseMessage = VoiceMessageDTO.builder()
                    .sessionId(sessionId)
                    .messageType("audio_response")
                    .content(response.getAudioResponse())
                    .audioFormat(response.getAudioFormat())
                    .timestamp(response.getTimestamp())
                    .isFromBot(true)
                    .build();
            
            messagingTemplate.convertAndSendToUser(userId, "/queue/voice", audioResponseMessage);
        }
        
        // Send emergency alert if detected
        if (response.getIsEmergency()) {
            VoiceMessageDTO emergencyMessage = VoiceMessageDTO.builder()
                    .sessionId(sessionId)
                    .messageType("emergency_alert")
                    .content(response.getEmergencyMessage())
                    .timestamp(System.currentTimeMillis())
                    .isFromBot(true)
                    .build();
            
            messagingTemplate.convertAndSendToUser(userId, "/queue/voice", emergencyMessage);
        }
    }

    private void sendErrorMessage(String sessionId, String errorMessage) {
        VoiceMessageDTO errorResponse = VoiceMessageDTO.builder()
                .sessionId(sessionId)
                .messageType("error")
                .content(errorMessage)
                .timestamp(System.currentTimeMillis())
                .isFromBot(true)
                .build();
        
        messagingTemplate.convertAndSend("/topic/voice/" + sessionId, errorResponse);
    }
}