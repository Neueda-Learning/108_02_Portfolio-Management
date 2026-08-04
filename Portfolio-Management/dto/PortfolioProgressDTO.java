package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Portfolio progress toward a configured target value")
public class PortfolioProgressDTO {
    private Long portfolioId;
    private String portfolioName;
    private String currency;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private BigDecimal remainingToTarget;
    private BigDecimal progressPercentage;
    private String status;
    private Integer suggestedMonthsToTarget;
    private BigDecimal estimatedMonthlyContributionNeeded;

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getRemainingToTarget() {
        return remainingToTarget;
    }

    public void setRemainingToTarget(BigDecimal remainingToTarget) {
        this.remainingToTarget = remainingToTarget;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSuggestedMonthsToTarget() {
        return suggestedMonthsToTarget;
    }

    public void setSuggestedMonthsToTarget(Integer suggestedMonthsToTarget) {
        this.suggestedMonthsToTarget = suggestedMonthsToTarget;
    }

    public BigDecimal getEstimatedMonthlyContributionNeeded() {
        return estimatedMonthlyContributionNeeded;
    }

    public void setEstimatedMonthlyContributionNeeded(BigDecimal estimatedMonthlyContributionNeeded) {
        this.estimatedMonthlyContributionNeeded = estimatedMonthlyContributionNeeded;
    }
}

