package com.example.dto;

import com.example.model.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuyAssetRequest(
		@NotNull(message = "Asset type is required")
		AssetType assetType,

		@NotBlank(message = "Symbol is required")
		String symbol,

		@NotBlank(message = "Name is required")
		String name,

		@NotNull(message = "Quantity is required")
		@DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
		BigDecimal quantity
) {}


