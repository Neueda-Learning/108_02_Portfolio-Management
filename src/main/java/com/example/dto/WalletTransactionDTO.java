package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionDTO(
        Long transactionId,
        Long userId,
        String transactionType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {}


