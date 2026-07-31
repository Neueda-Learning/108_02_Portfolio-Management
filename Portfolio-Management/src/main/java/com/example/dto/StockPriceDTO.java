package com.example.dto;

import java.math.BigDecimal;
import java.util.Map;

public record StockPriceDTO(
        String ticker,
        BigDecimal currentPrice,
        String currency,
        Long timestamp,
        Map<String, Object> additionalData
) {}
