package com.example.dto;

import java.math.BigDecimal;

public class PortfolioSummaryDTO {
    private Long portfolioId;
    private String portfolioName;
    private Integer totalItems;
    private BigDecimal totalInvestment;
    private BigDecimal currentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercentage;
    
    public PortfolioSummaryDTO() {
    }
    
    public PortfolioSummaryDTO(Long portfolioId, String portfolioName, Integer totalItems, 
                              BigDecimal totalInvestment, BigDecimal currentValue, 
                              BigDecimal totalProfitLoss, BigDecimal totalProfitLossPercentage) {
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.totalItems = totalItems;
        this.totalInvestment = totalInvestment;
        this.currentValue = currentValue;
        this.totalProfitLoss = totalProfitLoss;
        this.totalProfitLossPercentage = totalProfitLossPercentage;
    }
    
    public Long getPortfolioId() {
        return portfolioId;
    }
    
    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }
    
    public String getPortfolioName() {
        return portfolioName;
    }
    
    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }
    
    public Integer getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }
    
    public BigDecimal getTotalInvestment() {
        return totalInvestment;
    }
    
    public void setTotalInvestment(BigDecimal totalInvestment) {
        this.totalInvestment = totalInvestment;
    }
    
    public BigDecimal getCurrentValue() {
        return currentValue;
    }
    
    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }
    
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }
    
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }
    
    public BigDecimal getTotalProfitLossPercentage() {
        return totalProfitLossPercentage;
    }
    
    public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) {
        this.totalProfitLossPercentage = totalProfitLossPercentage;
    }
}

