package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.MessageResponse;
import com.bytehealers.healverse.dto.request.SendMessageRequest;
import com.bytehealers.healverse.model.Conversation;
import com.bytehealers.healverse.service.ChatService;
import com.bytehealers.healverse.service.IdempotencyService;
import com.bytehealers.healverse.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Slf4j
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final IdempotencyService idempotencyService;


    /**
     * Send a message in a conversation
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication, @CurrentUserId Long userId) {
        MessageResponse response = idempotencyService.execute(userId, idempotencyKey, MessageResponse.class,
                () -> chatService.sendMessage(conversationId, userId, request));
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", response));
    }

    /**
     * Get all messages for a conversation
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable String conversationId,
            Authentication authentication, @CurrentUserId Long userId) {

        List<MessageResponse> messages = chatService.getMessages(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully", messages));
    }

    /**
     * Get all conversations
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<Conversation>>> getAllConversations(@CurrentUserId Long userId) {
        List<Conversation> conversations = chatService.getAllConversations(userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
    }


}
