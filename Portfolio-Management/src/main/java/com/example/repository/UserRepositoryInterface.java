package com.example.repository;

import com.example.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryInterface {
    
    List<User> findAll();
    
    Optional<User> findById(Long userId);
    
    Optional<User> findByUsername(String username);
    
    boolean existsById(Long userId);
    
    boolean existsByUsername(String username);
    
    User save(User user);

    Optional<BigDecimal> getWalletBalance(Long userId);

    boolean addMoney(Long userId, BigDecimal amount);

    boolean removeMoney(Long userId, BigDecimal amount);
    
    void deleteById(Long userId);
}


