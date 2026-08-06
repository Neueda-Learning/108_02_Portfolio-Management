package com.example.repository;

import com.example.dto.WalletTransactionDTO;

import java.math.BigDecimal;
import java.util.List;

public interface WalletTransactionRepositoryInterface {
    WalletTransactionDTO save(Long userId, String transactionType, BigDecimal amount,
                              BigDecimal balanceBefore, BigDecimal balanceAfter);

    List<WalletTransactionDTO> findByUserId(Long userId);
}


