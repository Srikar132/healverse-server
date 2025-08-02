package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceChatMessageDTO {
    
    private String sessionId;
    private Long userId;
    private String messageType; // "user_text", "ai_response", "voice_start", "voice_end"
    private String content;
    private LocalDateTime timestamp;
    private String audioFormat; // e.g., "wav", "mp3"
    private byte[] audioData;
    private Boolean isProcessed;
    private String processingStatus; // "pending", "processing", "completed", "error"
}