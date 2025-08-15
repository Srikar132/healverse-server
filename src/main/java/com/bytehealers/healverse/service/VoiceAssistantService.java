package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.TextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class VoiceAssistantService {


    @Autowired
    private ChatService chatService;

    private static final Logger logger = LoggerFactory.getLogger(VoiceAssistantService.class);

    private static final String SYSTEM_PROMPT =
            "You are a fast and concise AI voice assistant. Keep responses conversational, " +
                    "short (max 2 sentences), friendly, avoid filler words. Be helpful and direct. " +
                    "Remember the context of our conversation.";

    private final ChatModel chatModel;
    private final OpenAiAudioSpeechModel audioSpeechModel;

    // Store conversation history per session/user
    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    public VoiceAssistantService(ChatModel chatModel, OpenAiAudioSpeechModel audioSpeechModel) {
        this.chatModel = chatModel;
        this.audioSpeechModel = audioSpeechModel;
    }

    public TextResponse processAIRequest(String text, String sessionId) {
        logger.info("Processing AI request for session: {} with text: {}", sessionId, text);

        try {
            long startTime = System.currentTimeMillis();

            // Get or create conversation history for this session
            List<Message> messages = getConversationHistory(sessionId);

            // Add user message
            UserMessage userMessage = new UserMessage(text);
            messages.add(userMessage);

            // Create prompt with conversation history
            Prompt prompt = new Prompt(messages);

            String aiResponse = chatModel.call(prompt).getResult().getOutput().getText();

            // Add AI response to conversation history
            AssistantMessage assistantMessage = new AssistantMessage(aiResponse);
            messages.add(assistantMessage);

            // Keep conversation history manageable (last 20 messages)
            if (messages.size() > 21) { // 1 system + 20 conversation messages
                messages.subList(1, messages.size() - 20).clear();
            }

            long endTime = System.currentTimeMillis();
            logger.info("AI request completed in {} ms for session: {}", (endTime - startTime), sessionId);

            return new TextResponse(aiResponse);

        } catch (Exception e) {
            logger.error("Error processing AI request for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Unable to process AI request", e);
        }
    }

    public byte[] generateSpeech(String text) {
        logger.info("Generating speech for text length: {}", text.length());

        try {
            long startTime = System.currentTimeMillis();

            byte[] audioBytes = audioSpeechModel.call(text);

            long endTime = System.currentTimeMillis();
            logger.info("TTS completed in {} ms, audio size: {} bytes",
                    (endTime - startTime), audioBytes.length);

            return audioBytes;

        } catch (Exception e) {
            logger.error("TTS error: {}", e.getMessage(), e);
            throw new RuntimeException("Text-to-speech conversion failed", e);
        }
    }

    public byte[] processVoiceChat(String text, String sessionId) {
        // Get AI response with conversation context
        TextResponse aiResponse = processAIRequest(text, sessionId);

        // Convert to speech
        return generateSpeech(aiResponse.getResponse());
    }

    private List<Message> getConversationHistory(String sessionId) {
        return conversationHistory.computeIfAbsent(sessionId, k -> {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            return messages;
        });
    }

    public void clearConversationHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        logger.info("Cleared conversation history for session: {}", sessionId);
    }

    public void clearAllConversations() {
        conversationHistory.clear();
        logger.info("Cleared all conversation histories");
    }

    public int getActiveSessionsCount() {
        return conversationHistory.size();
    }
}