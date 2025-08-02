package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessageDTO {
    private String sessionId;
    private String messageType; // "audio", "text", "control"
    private String content; // Base64 encoded audio or text content
    private String audioFormat; // "wav", "mp3", "webm"
    private Long timestamp;
    private String userId;
    private Boolean isFromBot;
}