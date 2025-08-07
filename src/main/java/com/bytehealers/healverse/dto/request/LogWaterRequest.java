package com.bytehealers.healverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LogWaterRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amountMl;

    private LocalDateTime loggedAt;

    // Constructors
    public LogWaterRequest() {}

    public LogWaterRequest(BigDecimal amountMl) {
        this.amountMl = amountMl;
        this.loggedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public BigDecimal getAmountMl() { return amountMl; }
    public void setAmountMl(BigDecimal amountMl) { this.amountMl = amountMl; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}