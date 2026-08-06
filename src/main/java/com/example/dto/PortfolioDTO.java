package com.example.dto;

import com.example.dto.PortfolioItemDTO;
import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PortfolioDTO {
    private Long id;
    private Long userId;
    private Long portfolioNumber;
    private String name;
    private String description;
    private String currency;
    private RiskLevel riskLevel;
    private InvestmentGoal investmentGoal;
    private BigDecimal targetValue;
    private InvestmentHorizon investmentHorizon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<com.example.dto.PortfolioItemDTO> items;

    public PortfolioDTO() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public InvestmentGoal getInvestmentGoal() { return investmentGoal; }
    public void setInvestmentGoal(InvestmentGoal investmentGoal) { this.investmentGoal = investmentGoal; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

    public InvestmentHorizon getInvestmentHorizon() { return investmentHorizon; }
    public void setInvestmentHorizon(InvestmentHorizon investmentHorizon) { this.investmentHorizon = investmentHorizon; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<com.example.dto.PortfolioItemDTO> getItems() { return items; }
    public void setItems(List<PortfolioItemDTO> items) { this.items = items; }
}

