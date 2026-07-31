package com.example.dto;

import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Rule-based recommendation payload for a portfolio")
public class PortfolioRecommendationDTO {
    private Long portfolioId;
    private String portfolioName;
    private String currency;
    private RiskLevel riskLevel;
    private InvestmentGoal investmentGoal;
    private InvestmentHorizon investmentHorizon;
    private PortfolioProgressDTO progress;
    private List<TargetAllocationDTO> targetAllocations;
    private List<TargetAllocationDTO> currentAllocations;
    private List<AllocationDriftDTO> allocationDrifts;
    private List<RecommendationItemDTO> recommendations;
    private String disclaimer;

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

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public InvestmentGoal getInvestmentGoal() {
        return investmentGoal;
    }

    public void setInvestmentGoal(InvestmentGoal investmentGoal) {
        this.investmentGoal = investmentGoal;
    }

    public InvestmentHorizon getInvestmentHorizon() {
        return investmentHorizon;
    }

    public void setInvestmentHorizon(InvestmentHorizon investmentHorizon) {
        this.investmentHorizon = investmentHorizon;
    }

    public PortfolioProgressDTO getProgress() {
        return progress;
    }

    public void setProgress(PortfolioProgressDTO progress) {
        this.progress = progress;
    }

    public List<TargetAllocationDTO> getTargetAllocations() {
        return targetAllocations;
    }

    public void setTargetAllocations(List<TargetAllocationDTO> targetAllocations) {
        this.targetAllocations = targetAllocations;
    }

    public List<TargetAllocationDTO> getCurrentAllocations() {
        return currentAllocations;
    }

    public void setCurrentAllocations(List<TargetAllocationDTO> currentAllocations) {
        this.currentAllocations = currentAllocations;
    }

    public List<AllocationDriftDTO> getAllocationDrifts() {
        return allocationDrifts;
    }

    public void setAllocationDrifts(List<AllocationDriftDTO> allocationDrifts) {
        this.allocationDrifts = allocationDrifts;
    }

    public List<RecommendationItemDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItemDTO> recommendations) {
        this.recommendations = recommendations;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

