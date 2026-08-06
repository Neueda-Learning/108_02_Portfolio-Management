package com.example.repository;

import com.example.model.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepositoryInterface {

    List<Portfolio> findAll();

    List<Portfolio> findByUserId(Long userId);

    Optional<Portfolio> findByUserIdAndPortfolioNumber(Long userId, Long portfolioNumber);

    Optional<Portfolio> findById(Long id);

    Optional<Portfolio> findByName(String name);

    boolean existsById(Long id);

    boolean existsByName(String name);

    Long getNextPortfolioNumberByUserId(Long userId);

    Portfolio save(Portfolio portfolio);

    void deleteById(Long id);
}

