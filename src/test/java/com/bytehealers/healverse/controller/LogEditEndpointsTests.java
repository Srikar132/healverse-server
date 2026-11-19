package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.exception.GlobalExceptionHandler;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.ExerciseLog;
import com.bytehealers.healverse.model.WaterLog;
import com.bytehealers.healverse.model.enums.ExerciseIntensity;
import com.bytehealers.healverse.security.CurrentUserId;
import com.bytehealers.healverse.service.CalorieCalculatorService;
import com.bytehealers.healverse.service.ExerciseLoggingService;
import com.bytehealers.healverse.service.IdempotencyService;
import com.bytehealers.healverse.service.WaterLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogEditEndpointsTests {

    private static final Long USER_ID = 5L;
    private static final LocalDateTime LOGGED_AT = LocalDateTime.of(2026, 1, 2, 8, 30);
    private static final String EXERCISE_BODY =
            "{\"exerciseName\":\"Cycling\",\"durationMinutes\":45,\"intensity\":\"HIGH\"}";

    private WaterLoggingService waterLoggingService;
    private ExerciseLoggingService exerciseLoggingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        waterLoggingService = mock(WaterLoggingService.class);
        exerciseLoggingService = mock(ExerciseLoggingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new WaterLoggingController(waterLoggingService, mock(IdempotencyService.class)),
                        new ExerciseLoggingController(exerciseLoggingService, mock(CalorieCalculatorService.class),
                                mock(IdempotencyService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new FixedUserIdResolver())
                .build();
    }

    @Test
    void waterUpdateReturnsTheUpdatedLog() throws Exception {
        WaterLog updated = new WaterLog(11L, null, new BigDecimal("500"), LOGGED_AT, LOGGED_AT);
        when(waterLoggingService.updateWaterLog(eq(11L), any(), eq(USER_ID))).thenReturn(updated);

        mockMvc.perform(put("/api/water-logs/11").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\": 500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.amountMl").value(500));
    }

    @Test
    void waterUpdateRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(put("/api/water-logs/11").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(waterLoggingService, never()).updateWaterLog(any(), any(), any());
    }

    @Test
    void waterUpdateRejectsMissingAmount() throws Exception {
        mockMvc.perform(put("/api/water-logs/11").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(waterLoggingService, never()).updateWaterLog(any(), any(), any());
    }

    @Test
    void waterUpdateOfMissingLogIsNotFound() throws Exception {
        when(waterLoggingService.updateWaterLog(eq(99L), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Water log not found"));

        mockMvc.perform(put("/api/water-logs/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\": 500}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Water log not found"));
    }

    @Test
    void waterDeleteSucceeds() throws Exception {
        when(waterLoggingService.deleteWaterLog(11L, USER_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/water-logs/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void waterDeleteOfMissingLogIsNotFound() throws Exception {
        when(waterLoggingService.deleteWaterLog(99L, USER_ID)).thenReturn(false);

        mockMvc.perform(delete("/api/water-logs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void exerciseUpdateReturnsTheUpdatedLog() throws Exception {
        ExerciseLog updated = new ExerciseLog(21L, null, "Cycling", 45, ExerciseIntensity.HIGH,
                new BigDecimal("420"), LOGGED_AT, LOGGED_AT);
        when(exerciseLoggingService.updateExerciseLog(eq(21L), any(), eq(USER_ID))).thenReturn(updated);

        mockMvc.perform(put("/api/exercise-logs/21").contentType(MediaType.APPLICATION_JSON)
                        .content(EXERCISE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caloriesBurned").value(420))
                .andExpect(jsonPath("$.data.exerciseName").value("Cycling"));
    }

    @Test
    void exerciseUpdateRejectsNonPositiveDuration() throws Exception {
        mockMvc.perform(put("/api/exercise-logs/21").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseName\":\"Cycling\",\"durationMinutes\":0,\"intensity\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());

        verify(exerciseLoggingService, never()).updateExerciseLog(any(), any(), any());
    }

    @Test
    void exerciseUpdateOfMissingLogIsNotFound() throws Exception {
        when(exerciseLoggingService.updateExerciseLog(eq(99L), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Exercise log not found"));

        mockMvc.perform(put("/api/exercise-logs/99").contentType(MediaType.APPLICATION_JSON)
                        .content(EXERCISE_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void exerciseDeleteSucceeds() throws Exception {
        when(exerciseLoggingService.deleteExerciseLog(21L, USER_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/exercise-logs/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void exerciseDeleteOfMissingLogIsNotFound() throws Exception {
        when(exerciseLoggingService.deleteExerciseLog(99L, USER_ID)).thenReturn(false);

        mockMvc.perform(delete("/api/exercise-logs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise log not found"));
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
