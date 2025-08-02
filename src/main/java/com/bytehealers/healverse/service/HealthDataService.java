package com.bytehealers.healverse.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 * Service to provide health data lookup functionality
 * This demonstrates function calling capabilities for the health chatbot
 */
@Service
public class HealthDataService {
    
    private final Map<String, String> symptomInfo;
    private final Map<String, String> medicationInfo;
    private final Map<String, String> healthTips;
    
    public HealthDataService() {
        // Initialize health data (in production, this would come from a database)
        this.symptomInfo = new HashMap<>();
        this.medicationInfo = new HashMap<>();
        this.healthTips = new HashMap<>();
        
        initializeSymptomInfo();
        initializeMedicationInfo();
        initializeHealthTips();
    }
    
    /**
     * Look up information about a specific symptom
     */
    public String getSymptomInfo(String symptom) {
        return symptomInfo.getOrDefault(symptom.toLowerCase(), 
            "I don't have specific information about that symptom. Please consult a healthcare professional for proper evaluation.");
    }
    
    /**
     * Get basic medication information
     */
    public String getMedicationInfo(String medication) {
        return medicationInfo.getOrDefault(medication.toLowerCase(),
            "I don't have information about that medication. Please consult your pharmacist or healthcare provider.");
    }
    
    /**
     * Get health tips for a specific topic
     */
    public String getHealthTips(String topic) {
        return healthTips.getOrDefault(topic.toLowerCase(),
            "Here are some general health tips: maintain a balanced diet, exercise regularly, get adequate sleep, and stay hydrated.");
    }
    
    /**
     * Find emergency contact information
     */
    public String getEmergencyContacts() {
        return """
            Emergency Contacts:
            • Emergency Services: 911 (US), 999 (UK), 112 (EU)
            • Poison Control: 1-800-222-1222 (US)
            • Mental Health Crisis: 988 (US Suicide & Crisis Lifeline)
            • Text Crisis Line: Text HOME to 741741
            """;
    }
    
    /**
     * Check for potential drug interactions (simplified)
     */
    public String checkDrugInteractions(String medication1, String medication2) {
        // This is a simplified example - in production, this would use a proper drug interaction database
        return "For accurate drug interaction information, please consult your pharmacist or use a professional drug interaction checker. Never mix medications without professional guidance.";
    }
    
    private void initializeSymptomInfo() {
        symptomInfo.put("headache", "Headaches can be caused by stress, dehydration, lack of sleep, or tension. If headaches are severe, persistent, or accompanied by other symptoms, seek medical attention.");
        symptomInfo.put("fever", "A fever is often your body's response to infection. Rest, stay hydrated, and monitor your temperature. Seek medical care if fever is high (over 103°F/39.4°C) or persistent.");
        symptomInfo.put("cough", "Coughs can be caused by infections, allergies, or irritants. A persistent cough lasting more than 2 weeks should be evaluated by a healthcare provider.");
        symptomInfo.put("fatigue", "Fatigue can result from poor sleep, stress, medical conditions, or lifestyle factors. If persistent fatigue affects daily activities, consult a healthcare provider.");
        symptomInfo.put("nausea", "Nausea can be caused by various factors including infections, medications, or motion. If accompanied by severe symptoms or persistent, seek medical evaluation.");
    }
    
    private void initializeMedicationInfo() {
        medicationInfo.put("ibuprofen", "Ibuprofen is a pain reliever and anti-inflammatory. Take with food to reduce stomach irritation. Do not exceed recommended dosage and consult a healthcare provider for long-term use.");
        medicationInfo.put("acetaminophen", "Acetaminophen (Tylenol) is a pain reliever and fever reducer. Do not exceed 4000mg in 24 hours. Avoid alcohol while taking acetaminophen.");
        medicationInfo.put("aspirin", "Aspirin is used for pain, fever, and inflammation. It can also be used for heart protection when prescribed. Should not be given to children due to Reye's syndrome risk.");
    }
    
    private void initializeHealthTips() {
        healthTips.put("sleep", "Aim for 7-9 hours of quality sleep nightly. Maintain a consistent sleep schedule, create a comfortable sleep environment, and avoid screens before bedtime.");
        healthTips.put("exercise", "Aim for 150 minutes of moderate aerobic activity weekly, plus muscle-strengthening activities twice a week. Start slowly and gradually increase intensity.");
        healthTips.put("nutrition", "Follow a balanced diet with plenty of fruits, vegetables, whole grains, and lean proteins. Limit processed foods, sugary drinks, and excessive sodium.");
        healthTips.put("stress", "Manage stress through regular exercise, meditation, deep breathing, adequate sleep, and social support. Chronic stress can impact physical health.");
        healthTips.put("hydration", "Drink 8-10 glasses of water daily, more if you're active or in hot weather. Water supports all body functions and helps maintain energy levels.");
    }
}