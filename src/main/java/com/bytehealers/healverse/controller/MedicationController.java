package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.dto.request.CreateMedicationRequest;
import com.bytehealers.healverse.dto.request.LogMedicationRequest;
import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.dto.response.MedicationResponse;
import com.bytehealers.healverse.service.MedicationService;
import com.bytehealers.healverse.util.UserContext;
import com.cloudinary.Api;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    @Autowired
    private UserContext  userContext;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicationResponse>>createMedication(
            @Valid @RequestBody CreateMedicationRequest request) {
        Long userId = userContext.getCurrentUserId();
        MedicationResponse response = medicationService.createMedication(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationResponse>>> getUserMedications() {
        Long userId = userContext.getCurrentUserId();
        List<MedicationResponse> medications = medicationService.getUserMedications(userId);
        return ResponseEntity.ok(ApiResponse.success(medications));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicationResponse>> updateMedication(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMedicationRequest request) {

        MedicationResponse response = medicationService.updateMedication(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMedication(@PathVariable UUID id) {
        medicationService.deleteMedication(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted medication"));
    }

    @PostMapping("/{id}/log")
    public ResponseEntity<ApiResponse<String>> logMedication(
            @PathVariable UUID id,
            @Valid @RequestBody LogMedicationRequest request) {

        try{

            medicationService.logMedication(id, request);

            return ResponseEntity.ok(ApiResponse.success("Log medication successfully!"));
        }catch(Exception e){
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to log"));
        }

    }
}