package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.HealthChatRequestDTO;
import com.bytehealers.healverse.dto.HealthChatResponseDTO;
import com.bytehealers.healverse.service.HealthChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/health-chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Configure appropriately for production
public class HealthChatController {
    
    private final HealthChatbotService healthChatbotService;
    
    @PostMapping("/query")
    public ResponseEntity<HealthChatResponseDTO> processHealthQuery(
            @Valid @RequestBody HealthChatRequestDTO request) {
        
        log.info("Received health query from user {} in session {}", 
                request.getUserId(), request.getSessionId());
        
        try {
            HealthChatResponseDTO response = healthChatbotService.processHealthQuery(request);
            
            if (response.getIsEmergency()) {
                log.warn("Emergency detected for session {}: {}", 
                        request.getSessionId(), request.getQuery());
                return ResponseEntity.status(202).body(response); // 202 Accepted for emergency
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing health query: ", e);
            return ResponseEntity.status(500).body(
                HealthChatResponseDTO.builder()
                    .sessionId(request.getSessionId())
                    .response("I'm experiencing technical difficulties. Please try again or consult a healthcare professional.")
                    .responseType("error")
                    .isEmergency(false)
                    .build()
            );
        }
    }
    
    @PostMapping("/session/start")
    public ResponseEntity<String> startSession(@RequestParam Long userId) {
        try {
            String sessionId = healthChatbotService.startNewSession(userId);
            log.info("Started new session {} for user {}", sessionId, userId);
            return ResponseEntity.ok(sessionId);
        } catch (Exception e) {
            log.error("Error starting session: ", e);
            return ResponseEntity.status(500).body("Error starting session");
        }
    }
    
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<String> endSession(@PathVariable String sessionId) {
        try {
            String summary = healthChatbotService.endSession(sessionId);
            log.info("Ended session {}", sessionId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error ending session: ", e);
            return ResponseEntity.status(500).body("Error ending session");
        }
    }
    
    @GetMapping("/session/{sessionId}/health-check")
    public ResponseEntity<String> healthCheck(@PathVariable String sessionId) {
        return ResponseEntity.ok("Health chat service is running for session: " + sessionId);
    }
}