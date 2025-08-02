# Health Chatbot API Documentation

## Overview
The Health Chatbot API provides intelligent health conversations using Spring AI and OpenAI integration. It offers both REST API endpoints and WebSocket support for real-time communication.

## Configuration

### Required Environment Variables
```bash
OPENAI_API_KEY=your-openai-api-key-here
```

### Application Properties
```properties
# Spring AI OpenAI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY:your-openai-api-key-here}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=1000
```

## REST API Endpoints

### 1. Start a New Health Chat Session
```bash
POST /api/health-chat/session/start?userId=1
```

**Response:**
```text
"session-uuid-here"
```

### 2. Send Health Query
```bash
POST /api/health-chat/query
Content-Type: application/json

{
  "sessionId": "session-uuid-here",
  "userId": 1,
  "query": "What are the benefits of regular exercise?",
  "healthTopicCategory": "fitness"
}
```

**Response:**
```json
{
  "sessionId": "session-uuid-here",
  "response": "Regular exercise provides numerous health benefits including improved cardiovascular health, stronger muscles and bones, better mental health, and enhanced immune function. I recommend consulting with your healthcare provider to design an exercise plan that's right for you.",
  "responseType": "general",
  "isEmergency": false,
  "disclaimer": "This information is for educational purposes only and not a substitute for professional medical advice.",
  "suggestedQuestions": [
    "How can I maintain good health?",
    "What are healthy lifestyle tips?",
    "When should I schedule check-ups?"
  ],
  "timestamp": "2025-08-02T11:40:00",
  "confidenceLevel": "high",
  "healthTopics": ["fitness"],
  "medicalDisclaimer": "This information is for educational purposes only and not a substitute for professional medical advice. Always consult healthcare providers for medical concerns."
}
```

### 3. End Session
```bash
POST /api/health-chat/session/{sessionId}/end
```

## Emergency Detection

The system automatically detects potential medical emergencies and responds appropriately:

```bash
POST /api/health-chat/query
Content-Type: application/json

{
  "sessionId": "session-uuid-here",
  "userId": 1,
  "query": "I have severe chest pain"
}
```

**Emergency Response (HTTP 202):**
```json
{
  "sessionId": "session-uuid-here",
  "response": "I detect you may be experiencing a medical emergency. Please:\n\n1. Call emergency services immediately (911 in the US)\n2. If unconscious or unable to call, have someone else call\n3. Stay calm and follow emergency operator instructions\n\nThis AI cannot replace emergency medical care. Please seek immediate professional help.",
  "responseType": "emergency_alert",
  "isEmergency": true,
  "disclaimer": "THIS IS NOT A SUBSTITUTE FOR EMERGENCY MEDICAL CARE",
  "timestamp": "2025-08-02T11:40:00",
  "emergencyContactInfo": "Emergency Services: 911",
  "emergencyInstructions": "Call emergency services immediately",
  "medicalDisclaimer": "SEEK IMMEDIATE MEDICAL ATTENTION"
}
```

## WebSocket Real-Time Chat

### Connection
```javascript
const socket = new WebSocket('ws://localhost:8080/ws/voice-chat');
```

### Send Message
```javascript
const message = {
  userId: 1,
  messageType: "user_text",
  content: "What is a healthy diet?",
  timestamp: new Date().toISOString()
};

socket.send(JSON.stringify(message));
```

### Receive Response
```javascript
socket.onmessage = function(event) {
  const response = JSON.parse(event.data);
  console.log('AI Response:', response.content);
};
```

## Health Topic Categories

The system supports specialized prompts for different health topics:

- **general**: General health information and wellness guidance
- **symptoms**: Symptom information and when to seek medical care
- **medication**: Medication information and safety guidelines
- **mental_health**: Mental health support with extra empathy
- **nutrition**: Nutrition and dietary information
- **fitness**: Exercise and fitness guidance

## Safety Features

1. **Medical Disclaimers**: All responses include appropriate medical disclaimers
2. **Emergency Detection**: Automatic detection of emergency keywords
3. **Professional Guidance**: Consistent encouragement to consult healthcare providers
4. **Boundary Setting**: Clear communication that the AI is not a doctor
5. **Crisis Response**: Special handling for mental health crisis indicators

## Error Handling

The API includes comprehensive error handling:

```json
{
  "sessionId": "session-uuid-here",
  "response": "I'm sorry, I'm having technical difficulties right now. Please try again or consult a healthcare professional if you have urgent health concerns.",
  "responseType": "error",
  "isEmergency": false,
  "medicalDisclaimer": "Technical error occurred - seek professional medical advice if needed"
}
```

## Example Integration

```javascript
// Start a session
const sessionResponse = await fetch('/api/health-chat/session/start?userId=1', {
  method: 'POST'
});
const sessionId = await sessionResponse.text();

// Send a health query
const queryResponse = await fetch('/api/health-chat/query', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    sessionId: sessionId,
    userId: 1,
    query: "I've been feeling tired lately. What could cause this?",
    healthTopicCategory: "symptoms"
  })
});

const healthAdvice = await queryResponse.json();
console.log('Health Assistant:', healthAdvice.response);

if (healthAdvice.isEmergency) {
  alert('EMERGENCY DETECTED: ' + healthAdvice.emergencyInstructions);
}
```

## Testing

Run the comprehensive test suite:

```bash
# Run all tests
./mvnw test

# Run specific test classes
./mvnw test -Dtest=SpringAIHealthChatbotServiceTest
./mvnw test -Dtest=HealthChatControllerTest
```