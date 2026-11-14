package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.*;
import com.bytehealers.healverse.dto.response.*;
import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.model.FoodItem;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.repo.FoodLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MimeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class FoodLoggingService {

    // log factory


    private final FoodLogRepository foodLogRepository;

    private final UserRepository userRepository;

    private final NutritionSyncService  nutritionSyncService;

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    private final GamificationService gamificationService;

    @Value("${app.ai.vision-model}")
    private String visionModel;

    @Value("${app.ai.max-tokens:4000}")
    private Integer maxTokens;

    private static final List<MimeType> SUPPORTED_IMAGE_TYPES = List.of(
            MimeTypeUtils.IMAGE_JPEG, MimeTypeUtils.IMAGE_PNG, MimeTypeUtils.parseMimeType("image/webp"));


    private static final String FOOD_ANALYSIS_PROMPT = """
            Analyze the following food description and provide detailed nutritional information.
            Return the response in the following JSON format:
            {
                "mealName": "Brief meal name",
                "description": "Detailed description",
                "items": [
                    {
                        "name": "food item name",
                        "quantity": estimated_quantity_number,
                        "unit": "g/ml/piece/cup etc",
                        "calories": estimated_calories,
                        "protein": protein_in_grams,
                        "fat": fat_in_grams,
                        "carbs": carbs_in_grams
                    }
                ]
            }
            
            Food Description: %s
            """;

    private static final String IMAGE_ANALYSIS_PROMPT = """
    Analyze the food in this image and return a JSON response with the following exact structure.
    Do not include any markdown formatting or code blocks - return only the raw JSON.
    
    {
        "mealName": "Name of the overall meal/dish",
        "description": "Brief description of the meal",
        "items": [
            {
                "name": "Food item name",
                "quantity": numeric_value,
                "unit": "g or ml or piece",
                "calories": numeric_value,
                "protein": numeric_value,
                "fat": numeric_value,
                "carbs": numeric_value
            }
        ]
    }
    
    Provide realistic nutritional values based on typical serving sizes. Return only the JSON, no other text.
    """;

    public FoodLogResponse logFoodByDescription(Long userId, FoodLogRequest request) {
        AIFoodResponse aiFoodResponse;
        try {
            String aiResponse = chatClient.prompt()
                    .user(String.format(FOOD_ANALYSIS_PROMPT, request.getDescription()))
                    .call()
                    .content();
            String cleanedResponse = cleanJsonResponse(aiResponse)
                    .orElseThrow(() -> new IllegalStateException("AI response was empty or invalid"));
            aiFoodResponse = objectMapper.readValue(cleanedResponse, AIFoodResponse.class);
        } catch (Exception e) {
            throw new ExternalServiceException("The AI service could not analyze the food description. Please try again.", e);
        }

        // TODO: gamification points are disabled here (StackOverflowError - investigate gamification recursion)
        return createFoodLog(userId, request, aiFoodResponse, false, null, null);
    }

    public FoodLogResponse logFoodByImage(Long userId, MultipartFile image, FoodLogRequest request) {
        MimeType imageType = supportedImageType(image);

        AIFoodResponse aiFoodResponse;
        try {
            String aiResponse = chatClient.prompt()
                    // Per-call override: photos go to the vision model, text keeps the default model.
                    .options(OpenAiChatOptions.builder()
                            .model(visionModel)
                            .maxCompletionTokens(maxTokens)
                            .temperature(0.2)
                            .build())
                    .user(userSpec -> userSpec
                            .text(IMAGE_ANALYSIS_PROMPT)
                            .media(imageType, image.getResource()))
                    .call()
                    .content();

            String cleanedResponse = cleanJsonResponse(aiResponse)
                    .orElseThrow(() -> new IllegalStateException("AI response was empty or invalid"));
            aiFoodResponse = objectMapper.readValue(cleanedResponse, AIFoodResponse.class);
        } catch (Exception e) {
            throw new ExternalServiceException("The AI service could not analyze the food image. Please try again.", e);
        }

        String imageUrl = "https://example.com/fake-image.jpg"; // Your actual image upload logic

        FoodLogResponse res = createFoodLog(userId, request, aiFoodResponse, true, imageUrl, aiFoodResponse.getDescription());
        nutritionSyncService.syncAfterFoodLogAsync(userId, res.getLoggedAt());

        // TODO: gamification points are disabled here (StackOverflowError - investigate gamification recursion)
        return res;
    }

    private static MimeType supportedImageType(MultipartFile image) {
        MimeType type;
        try {
            type = MimeTypeUtils.parseMimeType(String.valueOf(image.getContentType()));
        } catch (Exception e) {
            throw new BadRequestException("Unsupported image type. Use JPEG, PNG or WebP.");
        }
        return SUPPORTED_IMAGE_TYPES.stream()
                .filter(supported -> supported.equalsTypeAndSubtype(type))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported image type. Use JPEG, PNG or WebP."));
    }

    private Optional<String> cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return Optional.empty();
        }

        // Remove markdown code blocks
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        return Optional.of(response.trim());
    }
    private FoodLogResponse createFoodLog(Long userId, FoodLogRequest request, AIFoodResponse aiFoodResponse,
                                          boolean isFromCamera, String imageUrl, String imageDescription) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FoodLog foodLog = new FoodLog();
        foodLog.setUser(user);
        foodLog.setMealType(request.getMealType());
        foodLog.setMealName(aiFoodResponse.getMealName());
        foodLog.setImageUrl(imageUrl);
        foodLog.setImageDescription(imageDescription);
        foodLog.setLoggedAt(request.getLoggedAt() != null ? request.getLoggedAt() : LocalDateTime.now());
        foodLog.setCreatedAt(LocalDateTime.now());
        foodLog.setIsFromCamera(isFromCamera);

        List<FoodItem> foodItems = aiFoodResponse.getItems().stream()
                .map(aiItem -> {
                    FoodItem item = new FoodItem();
                    item.setFoodLog(foodLog);
                    item.setName(aiItem.getName());
                    item.setQuantity(aiItem.getQuantity());
                    item.setUnit(aiItem.getUnit());
                    item.setCalories(aiItem.getCalories());
                    item.setProtein(aiItem.getProtein());
                    item.setFat(aiItem.getFat());
                    item.setCarbs(aiItem.getCarbs());
                    return item;
                })
                .collect(Collectors.toList());

        foodLog.setItems(foodItems);

        FoodLog savedFoodLog = foodLogRepository.save(foodLog);
        return convertToResponse(savedFoodLog);
    }

    public List<FoodLogResponse> getTodaysFoodLogs(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<FoodLog> foodLogs = foodLogRepository.findTodaysFoodLogsByUserId(userId, startOfDay, endOfDay);
        return foodLogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<FoodLogResponse> getFoodLogsByDate(Long userId , LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<FoodLog> foodLogs = foodLogRepository.findTodaysFoodLogsByUserId(userId, startOfDay, endOfDay);
        return foodLogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }



    public List<FoodLogResponse> getAllFoodLogs(Long userId) {
        List<FoodLog> foodLogs = foodLogRepository.findByUserIdOrderByLoggedAtDesc(userId);
        return foodLogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public FoodLogResponse getFoodLogById(Long userId, Long foodLogId) {
        FoodLog foodLog = foodLogRepository.findById(foodLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Food log not found");
        }

        return convertToResponse(foodLog);
    }

    public FoodLogResponse updateFoodLog(Long userId, Long foodLogId, FoodLogRequest request) {
        FoodLog foodLog = foodLogRepository.findById(foodLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Food log not found");
        }

        foodLog.setMealType(request.getMealType());
        // Note: mealName is generated by AI, so we don't update it from request
        if (request.getLoggedAt() != null) {
            foodLog.setLoggedAt(request.getLoggedAt());
        }

        FoodLog savedFoodLog = foodLogRepository.save(foodLog);
        return convertToResponse(savedFoodLog);
    }

    public boolean deleteFoodLog(Long userId, Long foodLogId) {
        FoodLog foodLog = foodLogRepository.findById(foodLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Food log not found");
        }

        foodLogRepository.delete(foodLog);
        return true;
    }

    private FoodLogResponse convertToResponse(FoodLog foodLog) {
        FoodLogResponse response = new FoodLogResponse();
        response.setId(foodLog.getId());
        response.setMealType(foodLog.getMealType());
        response.setMealName(foodLog.getMealName());
        response.setImageUrl(foodLog.getImageUrl());
        response.setImageDescription(foodLog.getImageDescription());
        response.setLoggedAt(foodLog.getLoggedAt());
        response.setCreatedAt(foodLog.getCreatedAt());
        response.setIsFromCamera(foodLog.getIsFromCamera());

        List<FoodItemResponse> items = foodLog.getItems().stream()
                .map(item -> {
                    FoodItemResponse itemResponse = new FoodItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setName(item.getName());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setUnit(item.getUnit());
                    itemResponse.setCalories(item.getCalories());
                    itemResponse.setProtein(item.getProtein());
                    itemResponse.setFat(item.getFat());
                    itemResponse.setCarbs(item.getCarbs());
                    return itemResponse;
                })
                .collect(Collectors.toList());

        response.setItems(items);
        return response;
    }

    private String uploadImageToStorage(MultipartFile image) {
        // Implement your image upload logic here
        // This could be AWS S3, Google Cloud Storage, etc.
        // For now, returning a placeholder
        return "https://your-storage-service.com/images/" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
    }

    public List<FoodLog> getFoodLogsByDateRange(Long userId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime startOfDay = fromDate.atStartOfDay(); // 2025-08-07T00:00:00
        LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX); // 2025-08-07T23:59:59.999999999

        return foodLogRepository.findFoodLogsByUserIdAndDateRange(userId, startOfDay, endOfDay);
    }

}