package com.example.dto;

import com.example.model.AssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PortfolioItemDTO {
    private Long id;
    private Long portfolioId;
    private AssetType assetType;
    private String symbol;
    private String name;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDateTime purchaseDate;
    private String notes;
    private BigDecimal totalInvestment;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercentage;
    
    public PortfolioItemDTO() {
    }
    
    public PortfolioItemDTO(Long id, Long portfolioId, AssetType assetType, String symbol, String name, 
                           BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice, 
                           LocalDateTime purchaseDate, String notes, BigDecimal totalInvestment, 
                           BigDecimal currentValue, BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetType = assetType;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
        this.purchaseDate = purchaseDate;
        this.notes = notes;
        this.totalInvestment = totalInvestment;
        this.currentValue = currentValue;
        this.profitLoss = profitLoss;
        this.profitLossPercentage = profitLossPercentage;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPortfolioId() {
        return portfolioId;
    }
    
    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }
    
    public AssetType getAssetType() {
        return assetType;
    }
    
    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
    
    public BigDecimal getProfitLoss() {
        return profitLoss;
    }
    
    public void setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
    }
    
    public BigDecimal getProfitLossPercentage() {
        return profitLossPercentage;
    }
    
    public void setProfitLossPercentage(BigDecimal profitLossPercentage) {
        this.profitLossPercentage = profitLossPercentage;
    }
}

