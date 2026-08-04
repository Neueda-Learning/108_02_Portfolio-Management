package com.example.service;

import com.example.dto.WalletBalanceDTO;
import com.example.dto.WalletTransactionDTO;
import com.example.dto.WalletTransactionRequest;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.UserNotFoundException;
import com.example.repository.UserRepositoryInterface;
import com.example.repository.WalletTransactionRepositoryInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService implements WalletServiceInterface {

    private static final String DEPOSIT = "DEPOSIT";
    private static final String WITHDRAW = "WITHDRAW";

    private final UserRepositoryInterface userRepository;
    private final WalletTransactionRepositoryInterface walletTransactionRepository;

    public WalletService(UserRepositoryInterface userRepository,
                         WalletTransactionRepositoryInterface walletTransactionRepository) {
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public WalletBalanceDTO getWalletBalance(Long userId) {
        return new WalletBalanceDTO(userId, getBalanceOrThrow(userId));
    }

    @Override
    @Transactional
    public WalletBalanceDTO addMoney(Long userId, WalletTransactionRequest request) {
        BigDecimal before = getBalanceOrThrow(userId);
        if (!userRepository.addMoney(userId, request.amount())) {
            throw new UserNotFoundException(userId);
        }
        BigDecimal after = before.add(request.amount());
        walletTransactionRepository.save(userId, DEPOSIT, request.amount(), before, after);
        return new WalletBalanceDTO(userId, after);
    }

    @Override
    @Transactional
    public WalletBalanceDTO removeMoney(Long userId, WalletTransactionRequest request) {
        BigDecimal before = getBalanceOrThrow(userId);
        if (before.compareTo(request.amount()) < 0) {
            throw new InsufficientWalletBalanceException(userId, request.amount());
        }
        if (!userRepository.removeMoney(userId, request.amount())) {
            throw new InsufficientWalletBalanceException(userId, request.amount());
        }
        BigDecimal after = before.subtract(request.amount());
        walletTransactionRepository.save(userId, WITHDRAW, request.amount(), before, after);
        return new WalletBalanceDTO(userId, after);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionDTO> getTransactionHistory(Long userId) {
        getBalanceOrThrow(userId);
        return walletTransactionRepository.findByUserId(userId);
    }

    private BigDecimal getBalanceOrThrow(Long userId) {
        return userRepository.getWalletBalance(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}

