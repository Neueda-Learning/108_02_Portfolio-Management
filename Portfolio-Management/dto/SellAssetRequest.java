package com.example.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SellAssetRequest(
        @NotBlank(message = "Symbol is required")
        String symbol,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        // Optional override for non-market-tracked assets.
        @DecimalMin(value = "0.01", message = "Price per unit must be greater than 0")
        BigDecimal pricePerUnit
) {}

