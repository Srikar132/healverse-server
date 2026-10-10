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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/voice-chat")
@CrossOrigin(origins = "*")
public class VoiceAssistantController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceAssistantController.class);

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final VoiceAssistantService voiceAssistantService;
    private final UserContext userContext;

    public VoiceAssistantController(VoiceAssistantService voiceAssistantService, UserContext userContext) {
        this.voiceAssistantService = voiceAssistantService;
        this.userContext = userContext;
    }

    /**
     * Conversation history is keyed by authenticated user + client session id, so one user can never
     * read, extend or clear another user's conversation. A missing id falls back to the user's
     * "default" session.
     */
    private String sessionKey(String sessionId) {
        String clientSession = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId.trim();
        if (!SESSION_ID_PATTERN.matcher(clientSession).matches()) {
            throw new IllegalArgumentException("Invalid session id");
        }
        return userContext.getCurrentUserId() + ":" + clientSession;
    }

    @PostMapping("/ask-ai")
    public ResponseEntity<TextResponse> askAI(
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received ask-ai request");

        try {
            String sessionKey = sessionKey(sessionId);

            TextResponse response = voiceAssistantService.processAIRequest(request.getText(), sessionKey);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new TextResponse(null, "Invalid session id"));
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

        logger.info("Received voice-chat request");

        try {
            String sessionKey = sessionKey(sessionId);

            byte[] audioBytes = voiceAssistantService.processVoiceChat(request.getText(), sessionKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.set("Content-Disposition", "inline; filename=\"response.mp3\"");

            return ResponseEntity.ok().headers(headers).body(audioBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(null, "Invalid session id"));
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

        logger.info("Received voice-chat-with-text request");

        try {
            String sessionKey = sessionKey(sessionId);

            // Get AI response text
            TextResponse aiResponse = voiceAssistantService.processAIRequest(request.getText(), sessionKey);

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

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(null, "Invalid session id"));
        } catch (Exception e) {
            logger.error("Error in voiceChatWithText endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextResponse(request.getText(), "Voice chat failed"));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> clearConversation(@PathVariable String sessionId) {
        try {
            voiceAssistantService.clearConversationHistory(sessionKey(sessionId));
            return ResponseEntity.ok("Conversation history cleared");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid session id");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Voice Assistant is running!");
    }
}
