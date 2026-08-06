package com.example.exception;

import java.math.BigDecimal;

public class InsufficientWalletBalanceException extends RuntimeException {
    public InsufficientWalletBalanceException(Long userId, BigDecimal amount) {
        super("Insufficient wallet balance for user id " + userId + " to withdraw amount " + amount);
    }
}


