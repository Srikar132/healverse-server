package com.bytehealers.healverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private String id;
    private Long userId;
    private String title;
    private String createdAt;
    private String updatedAt;
    private List<MessageResponse> messages;
}