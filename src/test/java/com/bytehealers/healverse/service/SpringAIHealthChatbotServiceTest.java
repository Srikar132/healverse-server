package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAIHealthChatbotServiceTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private RetryTemplate aiRetryTemplate;

    @InjectMocks
    private SpringAIHealthChatbotService healthChatbotService;

    @Test
    void testStartNewSession() {
        Long userId = 1L;
        String sessionId = healthChatbotService.startNewSession(userId);
        
        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
    }

    @Test
    void testDetectEmergency() {
        // Test emergency detection
        assertTrue(healthChatbotService.detectEmergency("I have chest pain"));
        assertTrue(healthChatbotService.detectEmergency("I can't breathe"));
        assertTrue(healthChatbotService.detectEmergency("I want to kill myself"));
        
        // Test non-emergency
        assertFalse(healthChatbotService.detectEmergency("I have a headache"));
        assertFalse(healthChatbotService.detectEmergency("What is a healthy diet?"));
    }

    @Test
    void testEmergencyResponse() {
        HealthChatRequestDTO request = new HealthChatRequestDTO();
        request.setSessionId("test-session");
        request.setUserId(1L);
        request.setQuery("I have severe chest pain");
        
        HealthChatResponseDTO response = healthChatbotService.processHealthQuery(request);
        
        assertNotNull(response);
        assertTrue(response.getIsEmergency());
        assertEquals("emergency_alert", response.getResponseType());
        assertNotNull(response.getEmergencyContactInfo());
        assertTrue(response.getResponse().contains("emergency"));
    }

    @Test
    void testEndSession() {
        String sessionId = "test-session";
        String summary = healthChatbotService.endSession(sessionId);
        
        assertNotNull(summary);
        assertTrue(summary.contains("Thank you"));
    }
}