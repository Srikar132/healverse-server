package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.TextRequest;
import com.bytehealers.healverse.dto.response.TextResponse;
import com.bytehealers.healverse.dto.response.VoiceChatResponse;
import com.bytehealers.healverse.service.VoiceAssistantService;
import com.bytehealers.healverse.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/voice-chat")
@CrossOrigin(origins = "*")
public class VoiceAssistantController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceAssistantController.class);

    private final VoiceAssistantService voiceAssistantService;
    private final UserContext userContext;

    public VoiceAssistantController(VoiceAssistantService voiceAssistantService, UserContext userContext) {
        this.voiceAssistantService = voiceAssistantService;
        this.userContext = userContext;
    }

    @PostMapping("/ask-ai")
    public ResponseEntity<TextResponse> askAI(
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received ask-ai request for session: {}", sessionId);

        try {
//            Long userId = userContext.getCurrentUserId();
            // Use a default session if none provided
            String activeSessionId = sessionId != null ? sessionId : "default";

            TextResponse response = voiceAssistantService.processAIRequest(request.getText(), activeSessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in askAI endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TextResponse(null, "Unable to process request"));
        }
    }

    @PostMapping("/text-to-speech")
    public ResponseEntity<?> textToSpeech(@Valid @RequestBody TextRequest request) {
        logger.info("Received TTS request");

        try {
            byte[] audioBytes = voiceAssistantService.generateSpeech(request.getText());

            // Convert audio to base64
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // Return as JSON
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("audioBase64", audioBase64));

        } catch (Exception e) {
            logger.error("Error in textToSpeech endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(request.getText(), "TTS failed, returning text"));
        }
    }
    @PostMapping("/")
    public ResponseEntity<?> voiceChat(
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received voice-chat request for session: {}", sessionId);

        try {
//            Long userId =  userContext.getCurrentUserId();
            // Use a default session if none provided
            String activeSessionId = sessionId != null ? sessionId : "default";

            byte[] audioBytes = voiceAssistantService.processVoiceChat(request.getText(), activeSessionId );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.set("Content-Disposition", "inline; filename=\"response.mp3\"");

            return ResponseEntity.ok().headers(headers).body(audioBytes);

        } catch (Exception e) {
            logger.error("Error in voiceChat endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(request.getText(), "Voice chat failed, returning text"));
        }
    }

    @PostMapping("/voice-chat-with-text")
    public ResponseEntity<?> voiceChatWithText(
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received voice-chat-with-text request for session: {}", sessionId);

        try {
//            Long userId = userContext.getCurrentUserId();
            String activeSessionId = sessionId != null ? sessionId : "default";

            // Get AI response text
            TextResponse aiResponse = voiceAssistantService.processAIRequest(request.getText(), activeSessionId);

            // Generate audio from the response
            byte[] audioBytes = voiceAssistantService.generateSpeech(aiResponse.getResponse());

            // Convert audio to base64
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // Create combined response
            VoiceChatResponse response = new VoiceChatResponse();
            response.setTextResponse(aiResponse.getResponse());
            response.setAudioBase64(audioBase64);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            logger.error("Error in voiceChatWithText endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(request.getText(), "Voice chat failed"));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> clearConversation(@PathVariable String sessionId) {
        voiceAssistantService.clearConversationHistory(sessionId);
        return ResponseEntity.ok("Conversation history cleared for session: " + sessionId);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        int activeSessions = voiceAssistantService.getActiveSessionsCount();
        return ResponseEntity.ok("AI Voice Assistant is running! Active sessions: " + activeSessions);
    }
}