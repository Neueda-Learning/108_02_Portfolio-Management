package com.example.dto;

import java.math.BigDecimal;

public class MarketStockDTO {
    private String ticker;
    private String name;
    private String sector;
    private BigDecimal currentPrice;
    private String currency;
    private boolean priceAvailable;

    public MarketStockDTO() {
    }

    public MarketStockDTO(String ticker, String name, String sector, BigDecimal currentPrice, String currency, boolean priceAvailable) {
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.priceAvailable = priceAvailable;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isPriceAvailable() {
        return priceAvailable;
    }

    public void setPriceAvailable(boolean priceAvailable) {
        this.priceAvailable = priceAvailable;
    }
}

