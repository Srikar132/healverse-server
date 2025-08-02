# Voice Chat Backend API Documentation

## Overview
This API provides real-time voice communication capabilities for a health-focused chatbot. Users can speak to the bot and receive voice responses with built-in health safety features.

## Authentication
All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## REST API Endpoints

### Voice Chat Session Management

#### Create New Voice Chat Session
```http
POST /api/voice-chat/sessions
```

**Response:**
```json
{
  "message": "Voice chat session created successfully",
  "session": {
    "sessionId": "uuid",
    "userId": "user123",
    "status": "active",
    "startTime": 1640995200000,
    "messageCount": 0
  }
}
```

#### Get Session Details
```http
GET /api/voice-chat/sessions/{sessionId}
```

**Response:**
```json
{
  "sessionId": "uuid",
  "userId": "user123",
  "status": "active",
  "startTime": 1640995200000,
  "endTime": null,
  "messageCount": 5,
  "lastMessageType": "audio"
}
```

#### Update Session Status
```http
PUT /api/voice-chat/sessions/{sessionId}/status?status=paused
```

**Parameters:**
- `status`: "active", "paused", "ended"

#### End Session
```http
DELETE /api/voice-chat/sessions/{sessionId}
```

### Audio Processing

#### Transcribe Audio to Text
```http
POST /api/voice-chat/transcribe
Content-Type: multipart/form-data
```

**Parameters:**
- `audio`: Audio file (WAV, MP3, WebM)
- `format`: Audio format (optional, default: wav)

**Response:**
```json
{
  "message": "Audio transcribed successfully",
  "transcription": "Hello, I have a headache",
  "audioFormat": "wav",
  "audioSize": 12345
}
```

#### Convert Text to Speech
```http
POST /api/voice-chat/text-to-speech
```

**Parameters:**
- `text`: Text to convert
- `voice`: Voice type (optional, default: default)
- `format`: Audio format (optional, default: wav)

**Response:**
```json
{
  "message": "Text converted to speech successfully",
  "audioData": "base64-encoded-audio",
  "audioFormat": "wav",
  "voiceType": "default",
  "textLength": 25
}
```

### Health Chatbot

#### Process Health Query
```http
POST /api/voice-chat/health-query
```

**Parameters:**
- `sessionId`: Voice chat session ID
- `query`: Health-related question or concern

**Response:**
```json
{
  "message": "Health query processed successfully",
  "chatResponse": {
    "sessionId": "uuid",
    "responseText": "Headaches can have various causes...",
    "audioResponse": "base64-encoded-audio",
    "audioFormat": "wav",
    "hasDisclaimer": true,
    "disclaimerText": "Important: This chatbot provides...",
    "isEmergency": false,
    "emergencyMessage": null,
    "timestamp": 1640995200000
  }
}
```

#### Get Medical Disclaimer
```http
GET /api/voice-chat/health-disclaimer
```

**Response:**
```json
{
  "disclaimer": "Important: This chatbot provides general health information only..."
}
```

## WebSocket API

### Connection
Connect to WebSocket endpoint:
```
ws://localhost:8080/voice-chat
```

### Message Types

#### Send Audio Message
```json
{
  "sessionId": "uuid",
  "messageType": "audio",
  "content": "base64-encoded-audio",
  "audioFormat": "wav",
  "timestamp": 1640995200000,
  "userId": "user123",
  "isFromBot": false
}
```

#### Send Text Message
```json
{
  "sessionId": "uuid",
  "messageType": "text",
  "content": "I have a headache",
  "timestamp": 1640995200000,
  "userId": "user123",
  "isFromBot": false
}
```

#### Control Messages
```json
{
  "sessionId": "uuid",
  "messageType": "control",
  "content": "pause_session", // or "resume_session", "end_session"
  "userId": "user123"
}
```

### Received Message Types

#### Transcription Confirmation
```json
{
  "sessionId": "uuid",
  "messageType": "transcription",
  "content": "I have a headache",
  "timestamp": 1640995200000,
  "isFromBot": true
}
```

#### Bot Response
```json
{
  "sessionId": "uuid",
  "messageType": "response",
  "content": "Headaches can have various causes...",
  "timestamp": 1640995200000,
  "isFromBot": true
}
```

#### Audio Response
```json
{
  "sessionId": "uuid",
  "messageType": "audio_response",
  "content": "base64-encoded-audio",
  "audioFormat": "wav",
  "timestamp": 1640995200000,
  "isFromBot": true
}
```

#### Emergency Alert
```json
{
  "sessionId": "uuid",
  "messageType": "emergency_alert",
  "content": "⚠️ EMERGENCY DETECTED: If this is a medical emergency, please call 911...",
  "timestamp": 1640995200000,
  "isFromBot": true
}
```

## Health Safety Features

### Emergency Detection
The system automatically detects emergency keywords including:
- "emergency", "urgent", "911", "ambulance"
- "chest pain", "heart attack", "stroke"
- "can't breathe", "severe pain", "overdose"
- "suicide", "self harm", "bleeding heavily"
- "unconscious", "seizure", "choking"

### Medical Disclaimer
All health responses include a medical disclaimer emphasizing:
- Information is for educational purposes only
- Not a substitute for professional medical advice
- Users should consult healthcare providers
- Emergency situations require immediate medical attention

### Response Categories
The chatbot provides guidance on common health topics:
- **Symptoms**: Headaches, fever, general discomfort
- **Nutrition**: Diet advice, eating habits
- **Exercise**: Fitness recommendations, activity levels
- **Mental Health**: Stress management, anxiety support
- **General**: When to seek professional help

## Error Handling

### Common Error Responses
```json
{
  "error": "Session is not active"
}
```

```json
{
  "error": "Access denied to this session"
}
```

```json
{
  "error": "Audio file is required"
}
```

### HTTP Status Codes
- `200 OK`: Successful request
- `400 Bad Request`: Invalid parameters or missing data
- `401 Unauthorized`: Authentication required or invalid token
- `403 Forbidden`: Access denied to resource
- `404 Not Found`: Session or resource not found
- `500 Internal Server Error`: Server processing error

## Configuration

### Supported Audio Formats
- WAV (recommended)
- MP3
- WebM

### Session Limits
- Maximum session duration: 1 hour (configurable)
- Maximum audio file size: 10MB (configurable)
- Maximum concurrent sessions per user: 1

### Voice Settings
- Default voice type: "default"
- Supported audio output formats: WAV, MP3
- Real-time audio streaming via WebSocket

## Security Considerations

1. **Authentication**: All endpoints require valid JWT tokens
2. **Session Isolation**: Users can only access their own sessions
3. **Input Validation**: All audio and text inputs are validated
4. **Rate Limiting**: Consider implementing rate limiting for API calls
5. **Emergency Handling**: Automatic detection and alerting for emergency situations
6. **Medical Disclaimers**: Mandatory disclaimers for all health-related responses

## Integration Notes

### External Services
The implementation includes interfaces for:
- Speech-to-Text services (Google, OpenAI Whisper, Azure, AWS)
- Text-to-Speech services (Google, OpenAI TTS, Azure, AWS Polly)
- AI Chatbot services (OpenAI GPT, custom health models)

### Deployment
- Configure API keys for external services in environment variables
- Set up proper CORS policies for WebSocket connections
- Ensure adequate server resources for real-time audio processing
- Monitor session storage and cleanup inactive sessions