package com.example.dto;

import com.example.model.AssetType;
import java.math.BigDecimal;
import java.util.List;

public class AssetStatsDTO {
    private String ticker;
    private String name;
    private AssetType assetType;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal weekHigh;
    private BigDecimal weekLow;
    private BigDecimal monthHigh;
    private BigDecimal monthLow;
    private BigDecimal yearHigh;
    private BigDecimal yearLow;
    private List<PriceHistoryDTO> priceHistory;

    public AssetStatsDTO() {
    }

    // Getters and Setters
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

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(BigDecimal priceChange) {
        this.priceChange = priceChange;
    }

    public BigDecimal getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(BigDecimal priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(BigDecimal dayHigh) {
        this.dayHigh = dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
    }

    public void setDayLow(BigDecimal dayLow) {
        this.dayLow = dayLow;
    }

    public BigDecimal getWeekHigh() {
        return weekHigh;
    }

    public void setWeekHigh(BigDecimal weekHigh) {
        this.weekHigh = weekHigh;
    }

    public BigDecimal getWeekLow() {
        return weekLow;
    }

    public void setWeekLow(BigDecimal weekLow) {
        this.weekLow = weekLow;
    }

    public BigDecimal getMonthHigh() {
        return monthHigh;
    }

    public void setMonthHigh(BigDecimal monthHigh) {
        this.monthHigh = monthHigh;
    }

    public BigDecimal getMonthLow() {
        return monthLow;
    }

    public void setMonthLow(BigDecimal monthLow) {
        this.monthLow = monthLow;
    }

    public BigDecimal getYearHigh() {
        return yearHigh;
    }

    public void setYearHigh(BigDecimal yearHigh) {
        this.yearHigh = yearHigh;
    }

    public BigDecimal getYearLow() {
        return yearLow;
    }

    public void setYearLow(BigDecimal yearLow) {
        this.yearLow = yearLow;
    }

    public List<PriceHistoryDTO> getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(List<PriceHistoryDTO> priceHistory) {
        this.priceHistory = priceHistory;
    }
}

