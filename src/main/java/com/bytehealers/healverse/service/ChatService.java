package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.MessageResponse;
import com.bytehealers.healverse.dto.request.SendMessageRequest;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.ConversationRepository;
import com.bytehealers.healverse.repo.MessageRepository;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // Configuration
    @Value("${chat.history.max-messages:6}")
    private int maxHistoryMessages;

    @Value("${chat.context.max-tokens:2000}")
    private int maxContextTokens;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Cached system prompt template
    private String systemPromptTemplate;

    // Cache for user contexts to avoid repeated DB calls
    private final Map<Long, UserContextCache> userContextCache = new ConcurrentHashMap<>();

    // Cache TTL (5 minutes)
    private static final long CACHE_TTL = 5 * 60 * 1000;

    @PostConstruct
    public void init() {
        loadSystemPrompt();
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, Long userId, SendMessageRequest request) {
        // Get or create conversation
        Conversation conversation = getOrCreateConversation(conversationId, userId);

        // Save user message
        com.bytehealers.healverse.model.Message userMessage = new com.bytehealers.healverse.model.Message();
        userMessage.setConversation(conversation);
        userMessage.setContent(request.getContent());
        userMessage.setRole(MessageRole.USER);
        messageRepository.save(userMessage);

        // Generate bot response with optimized context
        String botResponse = generateBotResponse(conversation, userId, request.getContent());

        // Save bot message
        com.bytehealers.healverse.model.Message botMessage = new com.bytehealers.healverse.model.Message();
        botMessage.setConversation(conversation);
        botMessage.setContent(botResponse);
        botMessage.setRole(MessageRole.BOT);
        botMessage = messageRepository.save(botMessage);

        // Update conversation title if it's the first message
        if (conversation.getTitle() == null || conversation.getTitle().isEmpty()) {
            conversation.setTitle(generateConversationTitle(request.getContent()));
            conversationRepository.save(conversation);
        }

        return mapToMessageResponse(botMessage);
    }

    /**
     * Test OpenAI connection with a simple request
     */
    public String testOpenAIConnection() {
        try {
            log.info("Testing OpenAI connection...");
            
            String response = chatClient.prompt()
                    .user("Say 'Hello, OpenAI connection is working!'")
                    .call()
                    .content();
            
            log.info("OpenAI test successful: {}", response);
            return response;
        } catch (Exception e) {
            log.error("OpenAI test failed", e);
            throw new RuntimeException("OpenAI connection failed: " + e.getMessage(), e);
        }
    }

    public List<MessageResponse> getMessages(String conversationId, Long userId) {
        // Verify conversation belongs to user
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new RuntimeException("Conversation not found or access denied");
        }

        List<com.bytehealers.healverse.model.Message> messages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    private Conversation getOrCreateConversation(String conversationId, Long userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    Conversation newConversation = new Conversation();
                    newConversation.setId(conversationId);
                    newConversation.setUser(user);
                    return conversationRepository.save(newConversation);
                });
    }

    private String generateBotResponse(Conversation conversation, Long userId, String userMessage) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // Build optimized message chain
                List<Message> messages = buildOptimizedMessageChain(conversation, userId, userMessage);

                log.debug("Sending request to OpenAI with {} messages", messages.size());
                
                String response = chatClient.prompt()
                        .messages(messages)
                        .call()
                        .content();

                log.debug("Received response from OpenAI: {}", response != null ? "Success" : "Null response");
                return response;

            } catch (org.springframework.web.client.RestClientException e) {
                log.error("OpenAI API error (attempt {}/{}): {}", retryCount + 1, maxRetries, e.getMessage());
                
                if (e.getMessage().contains("Unexpected end-of-input") || 
                    e.getMessage().contains("JSON parse error")) {
                    log.error("JSON parsing error - possibly invalid API response");
                }
                
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error generating bot response (attempt {}/{})", retryCount + 1, maxRetries, e);
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("Failed to generate response after {} attempts", maxRetries);
        return "I apologize, but I'm having trouble processing your request right now. Please try again in a moment.";
    }

    private List<Message> buildOptimizedMessageChain(Conversation conversation, Long userId, String currentMessage) {
        List<Message> messages = new ArrayList<>();

        // 1. System message with user context
        String userContext = getCachedUserContext(userId);
        String systemPrompt = systemPromptTemplate.replace("{userContext}", userContext);
        messages.add(new SystemMessage(systemPrompt));

        // 2. Recent conversation history (optimized)
        List<Message> historyMessages = buildOptimizedHistory(conversation);
        messages.addAll(historyMessages);

        // 3. Current user message
        messages.add(new UserMessage(currentMessage));

        return messages;
    }

    private List<Message> buildOptimizedHistory(Conversation conversation) {
        List<com.bytehealers.healverse.model.Message> recentMessages =
                messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                        conversation.getId(), maxHistoryMessages);

        // Reverse to get chronological order
        Collections.reverse(recentMessages);

        List<Message> historyMessages = new ArrayList<>();
        int tokenCount = 0;

        for (com.bytehealers.healverse.model.Message msg : recentMessages) {
            // Estimate tokens (rough approximation: 1 token ≈ 4 characters)
            int messageTokens = msg.getContent().length() / 4;

            if (tokenCount + messageTokens > maxContextTokens) {
                break; // Stop if we exceed token limit
            }

            // Create appropriate message type
            if (msg.getRole() == MessageRole.USER) {
                historyMessages.add(new UserMessage(msg.getContent()));
            } else {
                historyMessages.add(new AssistantMessage(msg.getContent()));
            }

            tokenCount += messageTokens;
        }

        return historyMessages;
    }

    private String getCachedUserContext(Long userId) {
        UserContextCache cached = userContextCache.get(userId);
        long currentTime = System.currentTimeMillis();

        // Check if cache is valid
        if (cached != null && (currentTime - cached.timestamp) < CACHE_TTL) {
            return cached.context;
        }

        // Build fresh context
        String context = buildUserContext(userId);
        userContextCache.put(userId, new UserContextCache(context, currentTime));

        return context;
    }

    private String buildUserContext(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            return "User profile not available. Request profile completion for personalized Ayurvedic guidance.";
        }

        // Handle potential null values and convert BigDecimal to proper format
        String gender = profile.getGender() != null ? profile.getGender().toString() : "Unknown";
        Integer age = profile.getAge() != null ? profile.getAge() : 0;
        String height = profile.getHeightCm() != null ? profile.getHeightCm().toString() : "0";
        String currentWeight = profile.getCurrentWeightKg() != null ? profile.getCurrentWeightKg().toString() : "0";
        String targetWeight = profile.getTargetWeightKg() != null ? profile.getTargetWeightKg().toString() : "0";
        String activityLevel = profile.getActivityLevel() != null ? profile.getActivityLevel().getDescription() : "Unknown";
        String goal = profile.getGoal() != null ? profile.getGoal().getDescription() : "Unknown";
        String weightLossSpeed = profile.getWeightLossSpeed() != null ?
                " (" + profile.getWeightLossSpeed().getDescription() + ")" : "";
        String dietaryRestriction = profile.getDietaryRestriction() != null ?
                profile.getDietaryRestriction().getDescription() : "None";
        String healthCondition = profile.getHealthCondition() != null ?
                profile.getHealthCondition().getDescription() : "None";
        String otherHealth = profile.getOtherHealthConditionDescription() != null ?
                " + " + profile.getOtherHealthConditionDescription() : "";
        String address = profile.getAddress() != null && !profile.getAddress().trim().isEmpty() ?
                profile.getAddress() : "Not specified";

        // Compact format optimized for Ayurvedic context
        return String.format("""
            Profile: %s, %d yrs | %scm, %skg→%skg | %s | %s%s | Location: %s | Diet: %s | Health: %s%s
            """,
                gender, age, height, currentWeight, targetWeight,
                activityLevel, goal, weightLossSpeed, address, dietaryRestriction, healthCondition, otherHealth
        ).trim();
    }

    private void loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system-prompt-for-assistant.txt");
            this.systemPromptTemplate = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading system prompt", e);
            this.systemPromptTemplate = getDefaultSystemPrompt();
        }
    }

    private String generateConversationTitle(String firstMessage) {
        String title = firstMessage.length() > 50 ?
                firstMessage.substring(0, 50) + "..." : firstMessage;
        return title.replaceAll("[\\r\\n]+", " ").trim();
    }

    private MessageResponse mapToMessageResponse(com.bytehealers.healverse.model.Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getContent(),
                message.getRole().name(),
                message.getCreatedAt().format(FORMATTER)
        );
    }

    private String getDefaultSystemPrompt() {
        return """
            You are HealVerse AI, a knowledgeable and empathetic health and wellness assistant specializing in nutrition, fitness, and lifestyle guidance.

            **Core Guidelines:**
            - Always provide personalized advice based on the user's profile information
            - Use the user's current health data, goals, and preferences to tailor recommendations
            - Format all responses in clean, readable Markdown that renders well in mobile apps
            - Be supportive, motivational, and encouraging while remaining factual
            - Acknowledge limitations and recommend consulting healthcare professionals for medical concerns

            **User Context:**
            {userContext}

            Please provide helpful, personalized responses that address the user's questions while considering their profile and conversation history.
            """;
    }

    public List<Conversation> getAllConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    // Method to clear user cache when profile is updated
    public void clearUserCache(Long userId) {
        userContextCache.remove(userId);
    }

    // Method to clear all cache (for maintenance)
    public void clearAllCache() {
        userContextCache.clear();
    }

    // Inner class for caching user context
    private static class UserContextCache {
        final String context;
        final long timestamp;

        UserContextCache(String context, long timestamp) {
            this.context = context;
            this.timestamp = timestamp;
        }
    }
}