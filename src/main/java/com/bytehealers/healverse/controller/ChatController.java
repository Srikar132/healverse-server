package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.MessageResponse;
import com.bytehealers.healverse.dto.request.SendMessageRequest;
import com.bytehealers.healverse.model.Conversation;
import com.bytehealers.healverse.service.ChatService;
import com.bytehealers.healverse.util.UserContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserContext userContext;

    /**
     * Send a message in a conversation
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        try {
            Long userId = userContext.getCurrentUserId();
            MessageResponse response = chatService.sendMessage(conversationId, userId, request);
            return ResponseEntity.ok(ApiResponse.success("Message sent successfully", response));
        } catch (Exception e) {
            log.error("Error sending message for conversation: " + conversationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send message"));
        }
    }

    /**
     * Get all messages for a conversation
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable String conversationId,
            Authentication authentication) {

        try {
            Long userId = userContext.getCurrentUserId();
            List<MessageResponse> messages = chatService.getMessages(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully", messages));
        } catch (Exception e) {
            log.error("Error fetching messages for conversation: " + conversationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch messages"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Chat service is running", "OK"));
    }

    /**
     * Get all conversations
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<Conversation>>> getAllConversations() {
        try {
            Long userId = userContext.getCurrentUserId();
            List<Conversation> conversations = chatService.getAllConversations(userId);
            return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
        } catch (Exception e) {
            log.error("Error fetching all conversations", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch conversations"));
        }
    }
}
