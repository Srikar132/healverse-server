package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.UpdateFoodLogRequest;
import com.bytehealers.healverse.dto.response.FoodItemResponse;
import com.bytehealers.healverse.dto.response.FoodLogResponse;
import com.bytehealers.healverse.exception.GlobalExceptionHandler;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.enums.MealType;
import com.bytehealers.healverse.security.CurrentUserId;
import com.bytehealers.healverse.service.FoodLoggingService;
import com.bytehealers.healverse.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodLogUpdateEndpointTests {

    private static final Long USER_ID = 5L;
    private static final Long LOG_ID = 31L;
    private static final String ITEM =
            "{\"name\":\"Salad\",\"quantity\":150,\"unit\":\"g\",\"calories\":90,\"protein\":3,\"fat\":4,\"carbs\":10}";

    private FoodLoggingService foodLoggingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        foodLoggingService = mock(FoodLoggingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FoodLoggingController(foodLoggingService, mock(IdempotencyService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new FixedUserIdResolver())
                .build();
    }

    private static String body(String mealType, String items) {
        return "{" + (mealType == null ? "" : "\"mealType\":\"" + mealType + "\",") + "\"items\":" + items + "}";
    }

    private static String itemWith(String field, String value) {
        return ITEM.replaceAll("\"" + field + "\":(\"[^\"]*\"|[^,}]*)", "\"" + field + "\":" + value);
    }

    private static String oneItem(String field, String value) {
        return body("LUNCH", "[" + itemWith(field, value) + "]");
    }

    private void expectRejected(String json) throws Exception {
        mockMvc.perform(put("/api/food-logs/" + LOG_ID).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(foodLoggingService, never()).updateFoodLog(any(), any(), any());
    }

    @Test
    void validUpdateReturnsTheUpdatedLogAndPassesTheRequestToTheService() throws Exception {
        FoodItemResponse item = new FoodItemResponse();
        item.setId(1L);
        item.setName("Salad");
        FoodLogResponse updated = new FoodLogResponse();
        updated.setId(LOG_ID);
        updated.setMealType(MealType.DINNER);
        updated.setMealName("Light dinner");
        updated.setLoggedAt(LocalDateTime.of(2026, 1, 2, 19, 0));
        updated.setItems(List.of(item));
        when(foodLoggingService.updateFoodLog(eq(USER_ID), eq(LOG_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/food-logs/" + LOG_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"DINNER\",\"mealName\":\"Light dinner\","
                                + "\"loggedAt\":\"2026-01-02T19:00:00\",\"items\":[" + ITEM + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.mealName").value("Light dinner"))
                .andExpect(jsonPath("$.data.items[0].name").value("Salad"));

        ArgumentCaptor<UpdateFoodLogRequest> captor = ArgumentCaptor.forClass(UpdateFoodLogRequest.class);
        verify(foodLoggingService).updateFoodLog(eq(USER_ID), eq(LOG_ID), captor.capture());
        UpdateFoodLogRequest sent = captor.getValue();
        assertEquals(MealType.DINNER, sent.getMealType());
        assertEquals(LocalDateTime.of(2026, 1, 2, 19, 0), sent.getLoggedAt());
        assertEquals(150.0, sent.getItems().get(0).getQuantity());
    }

    @Test
    void mealNameAndTimeAreOptional() throws Exception {
        when(foodLoggingService.updateFoodLog(eq(USER_ID), eq(LOG_ID), any())).thenReturn(new FoodLogResponse());

        mockMvc.perform(put("/api/food-logs/" + LOG_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body("LUNCH", "[" + ITEM + "]")))
                .andExpect(status().isOk());
    }

    @Test
    void missingMealTypeIsRejected() throws Exception {
        expectRejected(body(null, "[" + ITEM + "]"));
    }

    @Test
    void unknownMealTypeIsRejected() throws Exception {
        expectRejected(body("MIDNIGHT", "[" + ITEM + "]"));
    }

    @Test
    void emptyItemsAreRejected() throws Exception {
        expectRejected(body("LUNCH", "[]"));
    }

    @Test
    void missingItemsAreRejected() throws Exception {
        expectRejected("{\"mealType\":\"LUNCH\"}");
    }

    @Test
    void moreThanThirtyItemsAreRejected() throws Exception {
        String items = IntStream.range(0, 31).mapToObj(i -> ITEM).collect(Collectors.joining(",", "[", "]"));

        expectRejected(body("LUNCH", items));
    }

    @Test
    void thirtyItemsAreAccepted() throws Exception {
        when(foodLoggingService.updateFoodLog(eq(USER_ID), eq(LOG_ID), any())).thenReturn(new FoodLogResponse());
        String items = IntStream.range(0, 30).mapToObj(i -> ITEM).collect(Collectors.joining(",", "[", "]"));

        mockMvc.perform(put("/api/food-logs/" + LOG_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body("LUNCH", items)))
                .andExpect(status().isOk());
    }

    @Test
    void zeroQuantityIsRejected() throws Exception {
        expectRejected(oneItem("quantity", "0"));
    }

    @Test
    void quantityAboveTheLimitIsRejected() throws Exception {
        expectRejected(oneItem("quantity", "100001"));
    }

    @Test
    void negativeCaloriesAreRejected() throws Exception {
        expectRejected(oneItem("calories", "-1"));
    }

    @Test
    void caloriesAboveTheLimitAreRejected() throws Exception {
        expectRejected(oneItem("calories", "20001"));
    }

    @Test
    void macrosAboveTheLimitAreRejected() throws Exception {
        expectRejected(oneItem("protein", "5001"));
        expectRejected(oneItem("fat", "5001"));
        expectRejected(oneItem("carbs", "5001"));
    }

    @Test
    void blankItemNameIsRejected() throws Exception {
        expectRejected(oneItem("name", "\"  \""));
    }

    @Test
    void blankItemUnitIsRejected() throws Exception {
        expectRejected(oneItem("unit", "\"\""));
    }

    @Test
    void overlongItemNameUnitAndMealNameAreRejected() throws Exception {
        expectRejected(oneItem("name", "\"" + "a".repeat(101) + "\""));
        expectRejected(oneItem("unit", "\"" + "a".repeat(21) + "\""));
        expectRejected("{\"mealType\":\"LUNCH\",\"mealName\":\"" + "a".repeat(101) + "\",\"items\":[" + ITEM + "]}");
    }

    @Test
    void anotherUsersOrMissingLogIsNotFound() throws Exception {
        when(foodLoggingService.updateFoodLog(eq(USER_ID), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Food log not found"));

        mockMvc.perform(put("/api/food-logs/99").contentType(MediaType.APPLICATION_JSON)
                        .content(body("LUNCH", "[" + ITEM + "]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Food log not found"));
    }

    private static class FixedUserIdResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUserId.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return USER_ID;
        }
    }
}
