package com.example.dto;

import java.math.BigDecimal;

public record PortfolioSummaryDTO(
        Long portfolioId,
        String portfolioName,
        Integer totalItems,
        BigDecimal totalInvestment,
        BigDecimal currentValue,
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitLossPercentage
) {}
