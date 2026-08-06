package com.example.controller;

import com.example.dto.WalletBalanceDTO;
import com.example.dto.WalletTransactionDTO;
import com.example.dto.WalletTransactionRequest;
import com.example.model.User;
import com.example.repository.UserRepositoryInterface;
import com.example.service.WalletServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserRepositoryInterface userRepository;
    private final WalletServiceInterface walletService;

    public UserController(UserRepositoryInterface userRepository, WalletServiceInterface walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a list of all users in the system")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their user ID")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieve a specific user by their username")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/wallet")
    @Operation(summary = "Get wallet balance", description = "Retrieve current wallet balance for a user")
    public ResponseEntity<WalletBalanceDTO> getWalletBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletBalance(userId));
    }

    @PostMapping("/{userId}/wallet/add")
    @Operation(summary = "Add money to wallet", description = "Deposit money into a user's wallet")
    public ResponseEntity<WalletBalanceDTO> addMoney(
            @PathVariable Long userId,
            @Valid @RequestBody WalletTransactionRequest request
    ) {
        return ResponseEntity.ok(walletService.addMoney(userId, request));
    }

    @PostMapping("/{userId}/wallet/remove")
    @Operation(summary = "Remove money from wallet", description = "Withdraw money from a user's wallet")
    public ResponseEntity<WalletBalanceDTO> removeMoney(
            @PathVariable Long userId,
            @Valid @RequestBody WalletTransactionRequest request
    ) {
        return ResponseEntity.ok(walletService.removeMoney(userId, request));
    }

    @GetMapping("/{userId}/wallet/transactions")
    @Operation(summary = "Get wallet transaction history", description = "Retrieve deposit and withdrawal history for a user wallet")
    public ResponseEntity<List<WalletTransactionDTO>> getWalletTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getTransactionHistory(userId));
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user in the system")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Delete a user from the system")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }
}

