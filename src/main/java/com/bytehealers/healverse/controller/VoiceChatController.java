package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.dto.VoiceChatSessionDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import com.bytehealers.healverse.service.SpeechToTextService;
import com.bytehealers.healverse.service.TextToSpeechService;
import com.bytehealers.healverse.service.VoiceChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice-chat")
@RequiredArgsConstructor
public class VoiceChatController {

    private final VoiceChatSessionService sessionService;
    private final SpeechToTextService speechToTextService;
    private final TextToSpeechService textToSpeechService;
    private final HealthChatbotService healthChatbotService;

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(Authentication authentication) {
        try {
            String userId = getUserIdFromAuth(authentication);
            VoiceChatSessionDTO session = sessionService.createSession(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Voice chat session created successfully");
            response.put("session", session);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating voice chat session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create session: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId, Authentication authentication) {
        try {
            VoiceChatSessionDTO session = sessionService.getSession(sessionId);
            
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify user owns this session
            String userId = getUserIdFromAuth(authentication);
            if (!userId.equals(session.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this session"));
            }
            
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error retrieving session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve session: " + e.getMessage()));
        }
    }

    @PutMapping("/sessions/{sessionId}/status")
    public ResponseEntity<?> updateSessionStatus(
            @PathVariable String sessionId,
            @RequestParam String status,
            Authentication authentication) {
        try {
            VoiceChatSessionDTO session = sessionService.getSession(sessionId);
            
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify user owns this session
            String userId = getUserIdFromAuth(authentication);
            if (!userId.equals(session.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this session"));
            }
            
            VoiceChatSessionDTO updatedSession = sessionService.updateSessionStatus(sessionId, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session status updated successfully");
            response.put("session", updatedSession);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating session {} status: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update session status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> endSession(@PathVariable String sessionId, Authentication authentication) {
        try {
            VoiceChatSessionDTO session = sessionService.getSession(sessionId);
            
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify user owns this session
            String userId = getUserIdFromAuth(authentication);
            if (!userId.equals(session.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this session"));
            }
            
            VoiceChatSessionDTO endedSession = sessionService.endSession(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session ended successfully");
            response.put("session", endedSession);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error ending session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to end session: " + e.getMessage()));
        }
    }

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "format", defaultValue = "wav") String audioFormat,
            Authentication authentication) {
        try {
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Audio file is required"));
            }
            
            String transcription = speechToTextService.convertSpeechToText(
                    audioFile.getInputStream(), audioFormat);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Audio transcribed successfully");
            response.put("transcription", transcription);
            response.put("audioFormat", audioFormat);
            response.put("audioSize", audioFile.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to transcribe audio: " + e.getMessage()));
        }
    }

    @PostMapping("/text-to-speech")
    public ResponseEntity<?> generateSpeech(
            @RequestParam("text") String text,
            @RequestParam(value = "voice", defaultValue = "default") String voiceType,
            @RequestParam(value = "format", defaultValue = "wav") String audioFormat,
            Authentication authentication) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Text is required"));
            }
            
            String audioData = textToSpeechService.convertTextToSpeech(text, voiceType, audioFormat);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Text converted to speech successfully");
            response.put("audioData", audioData);
            response.put("audioFormat", audioFormat);
            response.put("voiceType", voiceType);
            response.put("textLength", text.length());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error converting text to speech: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to convert text to speech: " + e.getMessage()));
        }
    }

    @PostMapping("/health-query")
    public ResponseEntity<?> processHealthQuery(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("query") String query,
            Authentication authentication) {
        try {
            String userId = getUserIdFromAuth(authentication);
            
            // Verify session exists and belongs to user
            VoiceChatSessionDTO session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!userId.equals(session.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this session"));
            }
            
            HealthChatResponseDTO chatResponse = healthChatbotService.processHealthQuery(sessionId, userId, query);
            
            // Update session message count
            sessionService.incrementMessageCount(sessionId, "text");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Health query processed successfully");
            response.put("chatResponse", chatResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing health query: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process health query: " + e.getMessage()));
        }
    }

    @GetMapping("/health-disclaimer")
    public ResponseEntity<?> getHealthDisclaimer() {
        try {
            String disclaimer = healthChatbotService.getMedicalDisclaimer();
            
            Map<String, Object> response = new HashMap<>();
            response.put("disclaimer", disclaimer);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving health disclaimer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve disclaimer"));
        }
    }

    private String getUserIdFromAuth(Authentication authentication) {
        return authentication.getName();
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}