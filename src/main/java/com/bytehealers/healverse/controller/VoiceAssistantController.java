package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.TextRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.TextResponse;
import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.dto.response.VoiceChatResponse;
import com.bytehealers.healverse.service.VoiceAssistantService;
import com.bytehealers.healverse.security.CurrentUserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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

    public VoiceAssistantController(VoiceAssistantService voiceAssistantService) {
        this.voiceAssistantService = voiceAssistantService;
    }

    /**
     * Conversation history is keyed by authenticated user + client session id, so one user can never
     * read, extend or clear another user's conversation. A missing id falls back to the user's
     * "default" session.
     */
    private String sessionKey(Long userId, String sessionId) {
        String clientSession = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId.trim();
        if (!SESSION_ID_PATTERN.matcher(clientSession).matches()) {
            throw new BadRequestException("Invalid session id");
        }
        return userId + ":" + clientSession;
    }

    @PostMapping("/ask-ai")
    public ResponseEntity<TextResponse> askAI(
            @CurrentUserId Long userId,
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received ask-ai request");
        return ResponseEntity.ok(voiceAssistantService.processAIRequest(request.getText(), sessionKey(userId, sessionId)));
    }

    @PostMapping("/text-to-speech")
    public ResponseEntity<Map<String, String>> textToSpeech(@Valid @RequestBody TextRequest request) {
        logger.info("Received TTS request");

        byte[] audioBytes = voiceAssistantService.generateSpeech(request.getText());
        return ResponseEntity.ok(Map.of("audioBase64", Base64.getEncoder().encodeToString(audioBytes)));
    }

    @PostMapping("/")
    public ResponseEntity<byte[]> voiceChat(
            @CurrentUserId Long userId,
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received voice-chat request");

        byte[] audioBytes = voiceAssistantService.processVoiceChat(request.getText(), sessionKey(userId, sessionId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.setContentLength(audioBytes.length);
        headers.set("Content-Disposition", "inline; filename=\"response.mp3\"");

        return ResponseEntity.ok().headers(headers).body(audioBytes);
    }

    @PostMapping("/voice-chat-with-text")
    public ResponseEntity<VoiceChatResponse> voiceChatWithText(
            @CurrentUserId Long userId,
            @Valid @RequestBody TextRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        logger.info("Received voice-chat-with-text request");

        TextResponse aiResponse = voiceAssistantService.processAIRequest(request.getText(), sessionKey(userId, sessionId));
        byte[] audioBytes = voiceAssistantService.generateSpeech(aiResponse.getResponse());

        VoiceChatResponse response = new VoiceChatResponse();
        response.setTextResponse(aiResponse.getResponse());
        response.setAudioBase64(Base64.getEncoder().encodeToString(audioBytes));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearConversation(@CurrentUserId Long userId, @PathVariable String sessionId) {
        voiceAssistantService.clearConversationHistory(sessionKey(userId, sessionId));
        return ResponseEntity.ok(ApiResponse.success("Conversation history cleared", null));
    }
}
