package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAIHealthChatbotService implements HealthChatbotService {
    
    private final OpenAiChatModel openAiChatModel;
    private final RetryTemplate aiRetryTemplate;
    private final HealthDataService healthDataService;
    
    // In-memory session storage (would be replaced with database in production)
    private final Map<String, LocalDateTime> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    // Emergency keywords for detection
    private static final Set<String> EMERGENCY_KEYWORDS = Set.of(
        "chest pain", "can't breathe", "difficulty breathing", "choking", "bleeding heavily",
        "severe pain", "unconscious", "overdose", "suicide", "self harm", "kill myself",
        "allergic reaction", "swelling", "can't swallow", "stroke", "heart attack",
        "emergency", "911", "ambulance", "help me", "dying"
    );
    
    @Override
    public HealthChatResponseDTO processHealthQuery(HealthChatRequestDTO request) {
        try {
            log.info("Processing health query for session: {}", request.getSessionId());
            
            // Check for emergency
            boolean isEmergency = detectEmergency(request.getQuery());
            
            if (isEmergency) {
                return handleEmergencyResponse(request);
            }
            
            // Get health-specific prompt based on category
            String systemPrompt = getHealthCategoryPrompt(request.getHealthTopicCategory());
            
            // Build the prompt with context
            String promptText = buildHealthPrompt(request, systemPrompt);
            
            // Execute AI query with retry logic
            ChatResponse response = aiRetryTemplate.execute(context -> {
                String fullPrompt = systemPrompt + "\n\nUser Query: " + promptText;
                Prompt prompt = new Prompt(fullPrompt);
                return openAiChatModel.call(prompt);
            });
            
            String aiResponse = response.getResult().getOutput().getText();
            
            // Store conversation in memory
            storeConversationContext(request.getSessionId(), request.getQuery(), aiResponse);
            
            return HealthChatResponseDTO.builder()
                .sessionId(request.getSessionId())
                .response(aiResponse)
                .responseType("general")
                .isEmergency(false)
                .disclaimer(getMedicalDisclaimer())
                .suggestedQuestions(generateSuggestedQuestions(request.getHealthTopicCategory()))
                .timestamp(LocalDateTime.now())
                .confidenceLevel("high")
                .healthTopics(extractHealthTopics(request.getQuery()))
                .medicalDisclaimer(getMedicalDisclaimer())
                .build();
                
        } catch (Exception e) {
            log.error("Error processing health query: ", e);
            return createErrorResponse(request.getSessionId(), e.getMessage());
        }
    }
    
    @Override
    public String startNewSession(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, LocalDateTime.now());
        sessionUserMap.put(sessionId, userId);
        log.info("Started new health chat session: {} for user: {}", sessionId, userId);
        return sessionId;
    }
    
    @Override
    public String endSession(String sessionId) {
        activeSessions.remove(sessionId);
        sessionUserMap.remove(sessionId);
        // Generate conversation summary (simplified)
        return "Thank you for using our health assistant. Remember to consult healthcare professionals for personalized medical advice.";
    }
    
    @Override
    public boolean detectEmergency(String query) {
        String lowerQuery = query.toLowerCase();
        return EMERGENCY_KEYWORDS.stream()
            .anyMatch(lowerQuery::contains);
    }
    
    private HealthChatResponseDTO handleEmergencyResponse(HealthChatRequestDTO request) {
        String emergencyResponse = """
            I detect you may be experiencing a medical emergency. Please:
            
            1. Call emergency services immediately (911 in the US)
            2. If unconscious or unable to call, have someone else call
            3. Stay calm and follow emergency operator instructions
            
            This AI cannot replace emergency medical care. Please seek immediate professional help.
            """;
        
        return HealthChatResponseDTO.builder()
            .sessionId(request.getSessionId())
            .response(emergencyResponse)
            .responseType("emergency_alert")
            .isEmergency(true)
            .disclaimer("THIS IS NOT A SUBSTITUTE FOR EMERGENCY MEDICAL CARE")
            .timestamp(LocalDateTime.now())
            .confidenceLevel("high")
            .emergencyContactInfo("Emergency Services: 911")
            .emergencyInstructions("Call emergency services immediately")
            .medicalDisclaimer("SEEK IMMEDIATE MEDICAL ATTENTION")
            .build();
    }
    
    private String getHealthCategoryPrompt(String category) {
        return switch (category != null ? category.toLowerCase() : "general") {
            case "symptoms" -> """
                Focus on general symptom information and when to seek medical care.
                Always emphasize the importance of professional medical evaluation.
                Do not attempt to diagnose conditions.
                """;
            case "medication" -> """
                Provide general medication information and safety guidelines.
                Always recommend consulting pharmacists or doctors for specific questions.
                Include warnings about drug interactions and side effects.
                """;
            case "mental_health" -> """
                Be especially empathetic and supportive for mental health topics.
                Provide resources and encourage professional mental health support.
                Watch for crisis indicators and provide appropriate crisis resources.
                """;
            case "nutrition" -> """
                Provide general nutrition and wellness information.
                Consider dietary restrictions and health conditions.
                Recommend consulting nutritionists for personalized meal plans.
                """;
            default -> """
                Provide general health information and wellness guidance.
                Encourage healthy lifestyle choices and preventive care.
                Always recommend professional medical consultation when appropriate.
                """;
        };
    }
    
    private String buildHealthPrompt(HealthChatRequestDTO request, String systemPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User Query: ").append(request.getQuery()).append("\n\n");
        
        if (request.getConversationContext() != null) {
            prompt.append("Conversation Context: ").append(request.getConversationContext()).append("\n\n");
        }
        
        // Add relevant health data if available
        String enhancedPrompt = enhancePromptWithHealthData(request.getQuery(), prompt.toString());
        
        prompt = new StringBuilder(enhancedPrompt);
        prompt.append("\nPlease provide a helpful, safe, and informative response.");
        
        return prompt.toString();
    }
    
    private String enhancePromptWithHealthData(String query, String originalPrompt) {
        StringBuilder enhanced = new StringBuilder(originalPrompt);
        
        // Check if the query is asking about specific symptoms, medications, or health topics
        String lowerQuery = query.toLowerCase();
        
        if (containsSymptomKeywords(lowerQuery)) {
            String symptomInfo = extractAndLookupSymptom(lowerQuery);
            if (symptomInfo != null) {
                enhanced.append("Relevant symptom information: ").append(symptomInfo).append("\n\n");
            }
        }
        
        if (containsMedicationKeywords(lowerQuery)) {
            String medicationInfo = extractAndLookupMedication(lowerQuery);
            if (medicationInfo != null) {
                enhanced.append("Relevant medication information: ").append(medicationInfo).append("\n\n");
            }
        }
        
        if (containsHealthTipKeywords(lowerQuery)) {
            String healthTips = extractAndLookupHealthTips(lowerQuery);
            if (healthTips != null) {
                enhanced.append("Relevant health tips: ").append(healthTips).append("\n\n");
            }
        }
        
        return enhanced.toString();
    }
    
    private boolean containsSymptomKeywords(String query) {
        return query.contains("headache") || query.contains("fever") || query.contains("cough") || 
               query.contains("fatigue") || query.contains("tired") || query.contains("nausea") ||
               query.contains("symptom");
    }
    
    private boolean containsMedicationKeywords(String query) {
        return query.contains("ibuprofen") || query.contains("acetaminophen") || query.contains("aspirin") ||
               query.contains("medication") || query.contains("medicine") || query.contains("drug") ||
               query.contains("tylenol") || query.contains("advil");
    }
    
    private boolean containsHealthTipKeywords(String query) {
        return query.contains("sleep") || query.contains("exercise") || query.contains("nutrition") ||
               query.contains("stress") || query.contains("hydration") || query.contains("diet") ||
               query.contains("tips") || query.contains("healthy");
    }
    
    private String extractAndLookupSymptom(String query) {
        if (query.contains("headache")) return healthDataService.getSymptomInfo("headache");
        if (query.contains("fever")) return healthDataService.getSymptomInfo("fever");
        if (query.contains("cough")) return healthDataService.getSymptomInfo("cough");
        if (query.contains("fatigue") || query.contains("tired")) return healthDataService.getSymptomInfo("fatigue");
        if (query.contains("nausea")) return healthDataService.getSymptomInfo("nausea");
        return null;
    }
    
    private String extractAndLookupMedication(String query) {
        if (query.contains("ibuprofen") || query.contains("advil")) return healthDataService.getMedicationInfo("ibuprofen");
        if (query.contains("acetaminophen") || query.contains("tylenol")) return healthDataService.getMedicationInfo("acetaminophen");
        if (query.contains("aspirin")) return healthDataService.getMedicationInfo("aspirin");
        return null;
    }
    
    private String extractAndLookupHealthTips(String query) {
        if (query.contains("sleep")) return healthDataService.getHealthTips("sleep");
        if (query.contains("exercise")) return healthDataService.getHealthTips("exercise");
        if (query.contains("nutrition") || query.contains("diet")) return healthDataService.getHealthTips("nutrition");
        if (query.contains("stress")) return healthDataService.getHealthTips("stress");
        if (query.contains("hydration")) return healthDataService.getHealthTips("hydration");
        return healthDataService.getHealthTips("general");
    }
    
    private void storeConversationContext(String sessionId, String query, String response) {
        // In a real implementation, this would store in a database
        // For now, we'll just log the conversation
        log.info("Conversation stored for session {}: Q: {} A: {}", 
                sessionId, query.substring(0, Math.min(50, query.length())), 
                response.substring(0, Math.min(50, response.length())));
    }
    
    private List<String> generateSuggestedQuestions(String category) {
        return switch (category != null ? category.toLowerCase() : "general") {
            case "symptoms" -> List.of(
                "When should I see a doctor?",
                "What are warning signs to watch for?",
                "How can I manage mild symptoms at home?"
            );
            case "medication" -> List.of(
                "What should I know about side effects?",
                "How do I safely store medications?",
                "What about drug interactions?"
            );
            case "mental_health" -> List.of(
                "What are some stress management techniques?",
                "How can I improve my sleep?",
                "Where can I find mental health resources?"
            );
            default -> List.of(
                "How can I maintain good health?",
                "What are healthy lifestyle tips?",
                "When should I schedule check-ups?"
            );
        };
    }
    
    private List<String> extractHealthTopics(String query) {
        // Simple keyword extraction - in production, this could use NLP
        List<String> topics = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("pain") || lowerQuery.contains("hurt")) topics.add("pain_management");
        if (lowerQuery.contains("sleep")) topics.add("sleep");
        if (lowerQuery.contains("stress") || lowerQuery.contains("anxiety")) topics.add("mental_health");
        if (lowerQuery.contains("diet") || lowerQuery.contains("nutrition")) topics.add("nutrition");
        if (lowerQuery.contains("exercise") || lowerQuery.contains("fitness")) topics.add("fitness");
        
        return topics.isEmpty() ? List.of("general_health") : topics;
    }
    
    private String getMedicalDisclaimer() {
        return "This information is for educational purposes only and not a substitute for professional medical advice. Always consult healthcare providers for medical concerns.";
    }
    
    private HealthChatResponseDTO createErrorResponse(String sessionId, String error) {
        return HealthChatResponseDTO.builder()
            .sessionId(sessionId)
            .response("I'm sorry, I'm having technical difficulties right now. Please try again or consult a healthcare professional if you have urgent health concerns.")
            .responseType("error")
            .isEmergency(false)
            .disclaimer(getMedicalDisclaimer())
            .timestamp(LocalDateTime.now())
            .confidenceLevel("low")
            .medicalDisclaimer("Technical error occurred - seek professional medical advice if needed")
            .build();
    }
}