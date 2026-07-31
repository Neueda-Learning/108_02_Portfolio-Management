package com.example.dto;

import com.example.model.AssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortfolioItemDTO(
        Long id,
        Long portfolioId,
        AssetType assetType,
        String symbol,
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        BigDecimal currentPrice,
        LocalDateTime purchaseDate,
        String notes,
        BigDecimal totalInvestment,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage
) {}
