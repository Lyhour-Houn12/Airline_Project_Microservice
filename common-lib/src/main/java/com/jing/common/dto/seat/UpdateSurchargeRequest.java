package com.jing.common.dto.seat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateSurchargeRequest(
        @NotNull(message = "surcharge is required")
        @DecimalMin(value = "0.00", message = "surcharge must be 0 or more")
        @Digits(integer = 8, fraction = 2, message = "surcharge must have at most 2 decimals")
        BigDecimal surcharge) {}