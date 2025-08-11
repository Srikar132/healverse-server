package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.response.*;
import com.bytehealers.healverse.dto.request.FoodLogRequest;
import com.bytehealers.healverse.model.FoodItem;
import com.bytehealers.healverse.model.FoodLog;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.repo.FoodLogRepository;
import com.bytehealers.healverse.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FoodLoggingService {

    @Autowired
    private FoodLogRepository foodLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NutritionSyncService  nutritionSyncService;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ObjectMapper objectMapper;

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
            Analyze the food in this image and provide detailed nutritional information.
            Identify all food items visible and estimate their quantities and nutritional values.
            Return the response in the following JSON format:
            {
                "mealName": "Brief meal name based on what you see",
                "description": "Detailed description of the food in the image",
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
            """;

    public FoodLogResponse logFoodByDescription(Long userId, FoodLogRequest request) {
        try {
            String prompt = String.format(FOOD_ANALYSIS_PROMPT, request.getDescription());

            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            AIFoodResponse aiFoodResponse = objectMapper.readValue(aiResponse, AIFoodResponse.class);

            return createFoodLog(userId, request, aiFoodResponse, false, null, null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze food description: " + e.getMessage());
        }
    }

    public FoodLogResponse logFoodByImage(Long userId, MultipartFile image, FoodLogRequest request) {
        try {
            // For now, skip AI/image processing
        /*
        String aiResponse = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(IMAGE_ANALYSIS_PROMPT)
                        .media(MimeTypeUtils.IMAGE_JPEG, image.getResource()))
                .call()
                .content();

        AIFoodResponse aiFoodResponse = objectMapper.readValue(aiResponse, AIFoodResponse.class);

        String imageUrl = uploadImageToStorage(image);
        */

            // --- Fake AI Response ---
            AIFoodResponse aiFoodResponse = new AIFoodResponse();
            aiFoodResponse.setMealName("Dummy Chicken Salad");
            aiFoodResponse.setDescription("A test description for fake chicken salad.");

            List<AIFoodResponse.AIFoodItem> items = new ArrayList<>();

            AIFoodResponse.AIFoodItem item1 = new AIFoodResponse.AIFoodItem();
            item1.setName("Chicken Breast");
            item1.setQuantity(150);
            item1.setUnit("g");
            item1.setCalories(165);
            item1.setProtein(31);
            item1.setFat(3.6);
            item1.setCarbs(0);
            items.add(item1);

            AIFoodResponse.AIFoodItem item2 = new AIFoodResponse.AIFoodItem();
            item2.setName("Lettuce");
            item2.setQuantity(50);
            item2.setUnit("g");
            item2.setCalories(8);
            item2.setProtein(0.5);
            item2.setFat(0.1);
            item2.setCarbs(1.6);
            items.add(item2);

            aiFoodResponse.setItems(items);

            String imageUrl = "https://example.com/fake-image.jpg"; // Fake placeholder URL

            FoodLogResponse res = createFoodLog(userId, request, aiFoodResponse, true, imageUrl, aiFoodResponse.getDescription());

            nutritionSyncService.syncAfterFoodLogAsync(userId , res.getLoggedAt());

            return res;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create fake food log: " + e.getMessage());
        }
    }


    private FoodLogResponse createFoodLog(Long userId, FoodLogRequest request, AIFoodResponse aiFoodResponse,
                                          boolean isFromCamera, String imageUrl, String imageDescription) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .orElseThrow(() -> new RuntimeException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to food log");
        }

        return convertToResponse(foodLog);
    }

    public FoodLogResponse updateFoodLog(Long userId, Long foodLogId, FoodLogRequest request) {
        FoodLog foodLog = foodLogRepository.findById(foodLogId)
                .orElseThrow(() -> new RuntimeException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to food log");
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
                .orElseThrow(() -> new RuntimeException("Food log not found"));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to food log");
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