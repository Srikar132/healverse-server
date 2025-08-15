package com.bytehealers.healverse.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceChatResponse {
    private String textResponse;
    private String audioBase64;

}