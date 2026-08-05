package com.example.dto;

import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating or updating a portfolio")
public class CreatePortfolioRequest {

    @Schema(description = "Owner user id for this portfolio", example = "1")
    private Long userId;

    @NotBlank(message = "Portfolio name is required")
    @Size(min = 2, max = 100, message = "Portfolio name must be between 2 and 100 characters")
    @Schema(description = "Unique display name of the portfolio", example = "My Tech Growth Portfolio")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Optional description of the portfolio's strategy or purpose",
            example = "Long-term growth portfolio focused on US technology stocks and ETFs")
    private String description;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code (e.g. USD, EUR)")
    @Schema(description = "Base currency for valuation (ISO 4217 code)", example = "USD", defaultValue = "USD")
    private String currency = "USD";

    @Schema(description = "Risk tolerance level of the portfolio",
            example = "MODERATE",
            allowableValues = {"CONSERVATIVE", "MODERATE", "AGGRESSIVE", "SPECULATIVE"})
    private RiskLevel riskLevel;

    @Schema(description = "Primary investment objective",
            example = "GROWTH",
            allowableValues = {"GROWTH", "INCOME", "CAPITAL_PRESERVATION", "BALANCED", "SPECULATION"})
    private InvestmentGoal investmentGoal;

    @DecimalMin(value = "0.0", inclusive = false, message = "Target value must be greater than 0")
    @Schema(description = "Target total portfolio value in the specified currency", example = "100000.00")
    private BigDecimal targetValue;

    @Schema(description = "Intended investment time horizon",
            example = "LONG_TERM",
            allowableValues = {"SHORT_TERM", "MEDIUM_TERM", "LONG_TERM"})
    private InvestmentHorizon investmentHorizon;

    public CreatePortfolioRequest() {
    }

    public CreatePortfolioRequest(Long userId, String name, String description, String currency,
                                   RiskLevel riskLevel, InvestmentGoal investmentGoal,
                                   BigDecimal targetValue, InvestmentHorizon investmentHorizon) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.currency = currency;
        this.riskLevel = riskLevel;
        this.investmentGoal = investmentGoal;
        this.targetValue = targetValue;
        this.investmentHorizon = investmentHorizon;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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
}

