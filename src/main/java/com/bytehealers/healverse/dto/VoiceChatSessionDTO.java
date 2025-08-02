package com.bytehealers.healverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceChatSessionDTO {
    private String sessionId;
    private String userId;
    private String status; // "active", "paused", "ended"
    private Long startTime;
    private Long endTime;
    private Integer messageCount;
    private String lastMessageType;
}