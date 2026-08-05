package com.example.model;

import com.example.model.Portfolio;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

public class User {

    private Long userId;
    private String username;
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private List<com.example.model.Portfolio> portfolios = new ArrayList<>();

    public User() {
    }

    public User(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public User(Long userId, String username, BigDecimal walletBalance) {
        this.userId = userId;
        this.username = username;
        this.walletBalance = walletBalance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public List<com.example.model.Portfolio> getPortfolios() {
        return portfolios;
    }

    public void setPortfolios(List<com.example.model.Portfolio> portfolios) {
        this.portfolios = portfolios;
    }

    public void addPortfolio(com.example.model.Portfolio portfolio) {
        portfolios.add(portfolio);
        portfolio.setUser(this);
    }

    public void removePortfolio(Portfolio portfolio) {
        portfolios.remove(portfolio);
        portfolio.setUser(null);
    }
}

