package com.example.dto;

import java.math.BigDecimal;
import java.util.Map;

public class StockPriceDTO {
    private String ticker;
    private BigDecimal currentPrice;
    private String currency;
    private Long timestamp;
    private Map<String, Object> additionalData;
    
    public StockPriceDTO() {
    }
    
    public StockPriceDTO(String ticker, BigDecimal currentPrice, String currency, Long timestamp, Map<String, Object> additionalData) {
        this.ticker = ticker;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.timestamp = timestamp;
        this.additionalData = additionalData;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
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
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }
}


