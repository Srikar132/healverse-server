package com.bytehealers.healverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class UpdateWaterLogRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amountMl;

    // Constructors
    public UpdateWaterLogRequest() {}

    // Getters and Setters
    public BigDecimal getAmountMl() { return amountMl; }
    public void setAmountMl(BigDecimal amountMl) { this.amountMl = amountMl; }
}
