package com.bytehealers.healverse.service.impl;

import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import com.bytehealers.healverse.service.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthChatbotServiceImpl implements HealthChatbotService {

    private final TextToSpeechService textToSpeechService;

    // Emergency keywords that trigger special handling
    private static final List<String> EMERGENCY_KEYWORDS = Arrays.asList(
        "emergency", "urgent", "911", "ambulance", "chest pain", "heart attack", 
        "stroke", "can't breathe", "severe pain", "overdose", "suicide", "self harm",
        "bleeding heavily", "unconscious", "seizure", "choking"
    );

    private static final String MEDICAL_DISCLAIMER = 
        "Important: This chatbot provides general health information only and is not a substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of your physician or other qualified health provider with any questions you may have regarding a medical condition. In case of emergency, call 911 immediately.";

    @Override
    public HealthChatResponseDTO processHealthQuery(String sessionId, String userId, String userMessage) {
        log.info("Processing health query for session: {} user: {}", sessionId, userId);
        
        boolean isEmergency = detectEmergency(userMessage);
        String responseText;
        String emergencyMessage = null;
        
        if (isEmergency) {
            emergencyMessage = "⚠️ EMERGENCY DETECTED: If this is a medical emergency, please call 911 or go to the nearest emergency room immediately. Do not rely on this chatbot for emergency medical assistance.";
            responseText = emergencyMessage + "\n\n" + generateHealthResponse(userMessage);
        } else {
            responseText = generateHealthResponse(userMessage);
        }
        
        // Add disclaimer for all health-related responses
        String fullResponse = responseText + "\n\n" + getMedicalDisclaimer();
        
        // Convert response to speech
        String audioResponse = textToSpeechService.convertTextToSpeech(fullResponse);
        
        return HealthChatResponseDTO.builder()
                .sessionId(sessionId)
                .responseText(fullResponse)
                .audioResponse(audioResponse)
                .audioFormat("wav")
                .hasDisclaimer(true)
                .disclaimerText(getMedicalDisclaimer())
                .isEmergency(isEmergency)
                .emergencyMessage(emergencyMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public boolean detectEmergency(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return EMERGENCY_KEYWORDS.stream()
                .anyMatch(lowerMessage::contains);
    }

    @Override
    public String getMedicalDisclaimer() {
        return MEDICAL_DISCLAIMER;
    }

    private String generateHealthResponse(String userMessage) {
        // TODO: Integrate with actual AI service like OpenAI GPT
        // This would send the user message to the AI with health-specific prompts
        
        // For now, provide a basic response based on common health topics
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("headache") || lowerMessage.contains("head pain")) {
            return "Headaches can have various causes including stress, dehydration, eye strain, or tension. For occasional headaches, ensure you're well-hydrated, get adequate rest, and manage stress. If headaches are frequent, severe, or accompanied by other symptoms, please consult a healthcare provider.";
        } else if (lowerMessage.contains("fever") || lowerMessage.contains("temperature")) {
            return "A fever is your body's natural response to infection. Stay hydrated, rest, and monitor your temperature. For adults, consider seeing a doctor if fever exceeds 103°F (39.4°C) or persists for more than 3 days. Seek immediate care if accompanied by severe symptoms.";
        } else if (lowerMessage.contains("diet") || lowerMessage.contains("nutrition") || lowerMessage.contains("eating")) {
            return "A balanced diet includes a variety of fruits, vegetables, whole grains, lean proteins, and healthy fats. Consider consulting with a registered dietitian for personalized nutrition advice based on your specific health needs and goals.";
        } else if (lowerMessage.contains("exercise") || lowerMessage.contains("workout") || lowerMessage.contains("fitness")) {
            return "Regular physical activity is important for overall health. The CDC recommends at least 150 minutes of moderate-intensity exercise per week. Start gradually and consult your healthcare provider before beginning a new exercise program, especially if you have existing health conditions.";
        } else if (lowerMessage.contains("stress") || lowerMessage.contains("anxiety") || lowerMessage.contains("mental health")) {
            return "Mental health is just as important as physical health. Stress management techniques include deep breathing, meditation, regular exercise, and maintaining social connections. If you're experiencing persistent anxiety or depression, please consider speaking with a mental health professional.";
        } else {
            return "Thank you for your health question. While I can provide general health information, I recommend discussing your specific concerns with a qualified healthcare provider who can give you personalized advice based on your medical history and current situation.";
        }
    }
}