package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.MessageResponse;
import com.bytehealers.healverse.dto.request.SendMessageRequest;
import com.bytehealers.healverse.model.*;
import com.bytehealers.healverse.repo.ConversationRepository;
import com.bytehealers.healverse.repo.MessageRepository;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional
    public MessageResponse sendMessage(String conversationId, Long userId, SendMessageRequest request) {
        // Get or create conversation
        Conversation conversation = getOrCreateConversation(conversationId, userId);


        // Save user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setContent(request.getContent());
        userMessage.setRole(MessageRole.USER);
        messageRepository.save(userMessage);

        // Generate bot response with user context
//        String botResponse = generateBotResponse(conversation, userId, request.getContent());
        String botResponse =
                "# 🌟 Daily Motivation 🌟\n" +
                        "\n" +
                        "## Keep Going 💪\n" +
                        "You're doing *amazing*! Don't forget to **take breaks**, stay **hydrated** 💧, and keep your _goals_ in sight!\n" +
                        "\n" +
                        "---\n" +
                        "### ✅ Here are a few tips to stay on track:\n" +
                        "- 📅 **Plan** your tasks for the day\n" +
                        "- 🍎 Eat **healthy snacks** to stay energized\n" +
                        "- 🧘‍♂️ Try short **meditation** or **stretching** sessions\n" +
                        "- 🎧 Listen to music to stay in the _zone_\n" +
                        "\n" +
                        "> *“Success is the sum of small efforts repeated day in and day out.”* – Robert Collier\n" +
                        "\n" +
                        "```javascript\n" +
                        "// Small progress every day\n" +
                        "let success = consistency + belief + hardWork;\n" +
                        "```\n" +
                        "\n" +
                        "👉 Learn more tips [here](https://www.healthline.com/health/mental-health-tips)\n" +
                        "\n" +
                        "---\n" +
                        "**You’ve got this!** 🌈✨";



        // Save bot message
        Message botMessage = new Message();
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

    public List<MessageResponse> getMessages(String conversationId, Long userId) {
        // Verify conversation belongs to user
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new RuntimeException("Conversation not found or access denied");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
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
                    newConversation.setTitle("New Chat");
                    return conversationRepository.save(newConversation);
                });
    }

    private String generateBotResponse(Conversation conversation, Long userId, String userMessage) {
        try {
            // Load system prompt template
            String systemPromptTemplate = loadSystemPrompt();

            // Get user context
            String userContext = buildUserContext(userId);

            // Get conversation history
            String conversationHistory = buildConversationHistory(conversation);

            // Prepare prompt variables
            Map<String, Object> promptVariables = new HashMap<>();
            promptVariables.put("userContext", userContext);
            promptVariables.put("conversationHistory", conversationHistory);
            promptVariables.put("currentMessage", userMessage);

            // Create and execute prompt
            PromptTemplate promptTemplate = new PromptTemplate(systemPromptTemplate);
            String finalPrompt = promptTemplate.render(promptVariables);

            return chatClient.prompt()
                    .user(finalPrompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating bot response", e);
            return "I apologize, but I'm having trouble processing your request right now. Please try again.";
        }
    }

    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system-prompt-for-assistant.txt");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading system prompt", e);
            return getDefaultSystemPrompt();
        }
    }

    private String buildUserContext(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            return "User profile not available. Please ask the user to complete their profile for personalized advice.";
        }

        StringBuilder context = new StringBuilder();
        context.append("User Profile Information:\n");
        context.append("- Gender: ").append(profile.getGender()).append("\n");
        context.append("- Age: ").append(profile.getAge()).append(" years\n");
        context.append("- Height: ").append(profile.getHeightCm()).append(" cm\n");
        context.append("- Current Weight: ").append(profile.getCurrentWeightKg()).append(" kg\n");
        context.append("- Target Weight: ").append(profile.getTargetWeightKg()).append(" kg\n");
        context.append("- Activity Level: ").append(profile.getActivityLevel().getDescription()).append("\n");
        context.append("- Goal: ").append(profile.getGoal().getDescription()).append("\n");

        if (profile.getWeightLossSpeed() != null) {
            context.append("- Weight Loss Speed: ").append(profile.getWeightLossSpeed().getDescription()).append("\n");
        }

        context.append("- Dietary Restriction: ").append(profile.getDietaryRestriction().getDescription()).append("\n");
        context.append("- Health Condition: ").append(profile.getHealthCondition().getDescription()).append("\n");

        if (profile.getOtherHealthConditionDescription() != null) {
            context.append("- Other Health Details: ").append(profile.getOtherHealthConditionDescription()).append("\n");
        }

        return context.toString();
    }

    private String buildConversationHistory(Conversation conversation) {
        List<Message> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // Limit to last 10 messages to avoid token limits
        List<Message> limitedMessages = recentMessages.stream()
                .skip(Math.max(0, recentMessages.size() - 10))
                .collect(Collectors.toList());

        StringBuilder history = new StringBuilder();
        history.append("Previous conversation context:\n");

        for (Message message : limitedMessages) {
            String role = message.getRole() == MessageRole.USER ? "User" : "Assistant";
            history.append(role).append(": ").append(message.getContent()).append("\n");
        }

        return history.toString();
    }

    private String generateConversationTitle(String firstMessage) {
        // Simple title generation - you can make this more sophisticated
        String title = firstMessage.length() > 50 ?
                firstMessage.substring(0, 50) + "..." :
                firstMessage;
        return title.replaceAll("[\\r\\n]+", " ").trim();
    }

    private MessageResponse mapToMessageResponse(Message message) {
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

            **Conversation History:**
            {conversationHistory}

            **Current User Message:**
            {currentMessage}

            Please provide a helpful, personalized response in Markdown format that addresses the user's question while considering their profile and conversation history.
            """;
    }

    public List<Conversation> getAllConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}
