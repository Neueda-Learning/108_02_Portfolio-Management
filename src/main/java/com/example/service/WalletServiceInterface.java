package com.example.service;

import com.example.dto.WalletBalanceDTO;
import com.example.dto.WalletTransactionDTO;
import com.example.dto.WalletTransactionRequest;

import java.util.List;

public interface WalletServiceInterface {
    WalletBalanceDTO getWalletBalance(Long userId);

    WalletBalanceDTO addMoney(Long userId, WalletTransactionRequest request);

    WalletBalanceDTO removeMoney(Long userId, WalletTransactionRequest request);

    List<WalletTransactionDTO> getTransactionHistory(Long userId);
}

