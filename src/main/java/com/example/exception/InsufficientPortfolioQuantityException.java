package com.example.exception;

import java.math.BigDecimal;

public class InsufficientPortfolioQuantityException extends RuntimeException {
    public InsufficientPortfolioQuantityException(Long portfolioId, String symbol, BigDecimal requestedQuantity, BigDecimal availableQuantity) {
        super("Insufficient quantity for symbol " + symbol + " in portfolio id " + portfolioId +
                ". Requested: " + requestedQuantity + ", available: " + availableQuantity);
    }
}


