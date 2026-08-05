package com.example.model;

import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.PortfolioItem;
import com.example.model.RiskLevel;
import com.example.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Portfolio {

    private Long id;
    private com.example.model.User user;
    private Long userId;
    private Long portfolioNumber;
    private String name;
    private String description;
    private String currency = "USD";
    private com.example.model.RiskLevel riskLevel;
    private com.example.model.InvestmentGoal investmentGoal;
    private BigDecimal targetValue;
    private com.example.model.InvestmentHorizon investmentHorizon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<com.example.model.PortfolioItem> items = new ArrayList<>();

    public Portfolio() {
    }

    public Portfolio(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt, List<com.example.model.PortfolioItem> items) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public com.example.model.User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPortfolioNumber() { return portfolioNumber; }
    public void setPortfolioNumber(Long portfolioNumber) { this.portfolioNumber = portfolioNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public com.example.model.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public com.example.model.InvestmentGoal getInvestmentGoal() { return investmentGoal; }
    public void setInvestmentGoal(InvestmentGoal investmentGoal) { this.investmentGoal = investmentGoal; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

    public com.example.model.InvestmentHorizon getInvestmentHorizon() { return investmentHorizon; }
    public void setInvestmentHorizon(InvestmentHorizon investmentHorizon) { this.investmentHorizon = investmentHorizon; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<com.example.model.PortfolioItem> getItems() { return items; }
    public void setItems(List<com.example.model.PortfolioItem> items) { this.items = items; }

    public void addItem(com.example.model.PortfolioItem item) {
        items.add(item);
        item.setPortfolio(this);
    }

    public void removeItem(PortfolioItem item) {
        items.remove(item);
        item.setPortfolio(null);
    }
}
