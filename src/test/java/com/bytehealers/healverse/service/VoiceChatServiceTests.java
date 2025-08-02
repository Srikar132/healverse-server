package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.dto.VoiceChatSessionDTO;
import com.bytehealers.healverse.service.impl.HealthChatbotServiceImpl;
import com.bytehealers.healverse.service.impl.SpeechToTextServiceImpl;
import com.bytehealers.healverse.service.impl.TextToSpeechServiceImpl;
import com.bytehealers.healverse.service.impl.VoiceChatSessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VoiceChatServiceTests {

    @Test
    void testVoiceChatSessionCreation() {
        VoiceChatSessionService sessionService = new VoiceChatSessionServiceImpl();
        
        VoiceChatSessionDTO session = sessionService.createSession("user123");
        
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals("user123", session.getUserId());
        assertEquals("active", session.getStatus());
        assertNotNull(session.getStartTime());
        assertEquals(0, session.getMessageCount());
    }

    @Test
    void testHealthChatbotEmergencyDetection() {
        TextToSpeechService ttsService = new TextToSpeechServiceImpl();
        HealthChatbotService chatbotService = new HealthChatbotServiceImpl(ttsService);
        
        // Test emergency detection
        assertTrue(chatbotService.detectEmergency("I'm having chest pain"));
        assertTrue(chatbotService.detectEmergency("This is an emergency"));
        assertTrue(chatbotService.detectEmergency("Call 911"));
        
        // Test non-emergency messages
        assertFalse(chatbotService.detectEmergency("I have a headache"));
        assertFalse(chatbotService.detectEmergency("What should I eat for breakfast?"));
    }

    @Test
    void testHealthChatbotResponse() {
        TextToSpeechService ttsService = new TextToSpeechServiceImpl();
        HealthChatbotService chatbotService = new HealthChatbotServiceImpl(ttsService);
        
        HealthChatResponseDTO response = chatbotService.processHealthQuery(
            "session123", "user123", "I have a headache");
        
        assertNotNull(response);
        assertEquals("session123", response.getSessionId());
        assertNotNull(response.getResponseText());
        assertNotNull(response.getAudioResponse());
        assertTrue(response.getHasDisclaimer());
        assertNotNull(response.getDisclaimerText());
        assertFalse(response.getIsEmergency());
    }

    @Test
    void testSpeechToTextService() {
        SpeechToTextService sttService = new SpeechToTextServiceImpl();
        
        // Use valid base64 data
        String validBase64 = "SGVsbG8gV29ybGQ="; // "Hello World" in base64
        String result = sttService.convertSpeechToText(validBase64, "wav");
        
        assertNotNull(result);
        assertTrue(result.contains("Audio received"));
        assertTrue(result.contains("wav format"));
    }

    @Test
    void testTextToSpeechService() {
        TextToSpeechService ttsService = new TextToSpeechServiceImpl();
        
        String result = ttsService.convertTextToSpeech("Hello world");
        
        assertNotNull(result);
        // Should return base64 encoded data
        assertFalse(result.isEmpty());
    }

    @Test
    void testHealthDisclaimerPresence() {
        TextToSpeechService ttsService = new TextToSpeechServiceImpl();
        HealthChatbotService chatbotService = new HealthChatbotServiceImpl(ttsService);
        
        String disclaimer = chatbotService.getMedicalDisclaimer();
        
        assertNotNull(disclaimer);
        assertTrue(disclaimer.contains("not a substitute for professional medical advice"));
        assertTrue(disclaimer.contains("call 911"));
    }
}