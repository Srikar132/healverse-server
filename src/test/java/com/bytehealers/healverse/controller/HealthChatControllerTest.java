package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthChatController.class)
class HealthChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthChatbotService healthChatbotService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProcessHealthQuery() throws Exception {
        // Prepare request
        HealthChatRequestDTO request = new HealthChatRequestDTO();
        request.setSessionId("test-session");
        request.setUserId(1L);
        request.setQuery("What is a healthy diet?");
        request.setHealthTopicCategory("nutrition");

        // Mock response
        HealthChatResponseDTO mockResponse = HealthChatResponseDTO.builder()
            .sessionId("test-session")
            .response("A healthy diet includes plenty of fruits, vegetables, whole grains, and lean proteins.")
            .responseType("general")
            .isEmergency(false)
            .timestamp(LocalDateTime.now())
            .confidenceLevel("high")
            .build();

        when(healthChatbotService.processHealthQuery(any(HealthChatRequestDTO.class)))
            .thenReturn(mockResponse);

        // Perform test
        mockMvc.perform(post("/api/health-chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session"))
                .andExpect(jsonPath("$.response").value("A healthy diet includes plenty of fruits, vegetables, whole grains, and lean proteins."))
                .andExpect(jsonPath("$.responseType").value("general"))
                .andExpect(jsonPath("$.isEmergency").value(false));
    }

    @Test
    void testStartSession() throws Exception {
        when(healthChatbotService.startNewSession(1L))
            .thenReturn("new-session-id");

        mockMvc.perform(post("/api/health-chat/session/start")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("new-session-id"));
    }
}