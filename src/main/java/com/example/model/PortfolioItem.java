package com.example.model;

import com.example.model.AssetType;
import com.example.model.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PortfolioItem {

    private Long id;
    private com.example.model.Portfolio portfolio;
    private com.example.model.AssetType assetType;
    private String symbol;
    private String name;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDateTime purchaseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;

    public PortfolioItem() {
    }

    public PortfolioItem(Long id, com.example.model.Portfolio portfolio, com.example.model.AssetType assetType, String symbol, String name,
                         BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice,
                         LocalDateTime purchaseDate, LocalDateTime createdAt, LocalDateTime updatedAt, String notes) {
        this.id = id;
        this.portfolio = portfolio;
        this.assetType = assetType;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
        this.purchaseDate = purchaseDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public com.example.model.Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public com.example.model.AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getTotalInvestment() {
        return purchasePrice.multiply(quantity);
    }

    public BigDecimal getCurrentValue() {
        if (currentPrice != null) {
            return currentPrice.multiply(quantity);
        }
        return purchasePrice.multiply(quantity);
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getTotalInvestment());
    }

    public BigDecimal getProfitLossPercentage() {
        BigDecimal totalInvestment = getTotalInvestment();
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getProfitLoss()
                .divide(totalInvestment, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}

