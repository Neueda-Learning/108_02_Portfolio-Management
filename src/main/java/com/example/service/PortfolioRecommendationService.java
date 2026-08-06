package com.example.service;

import com.example.dto.AllocationDriftDTO;
import com.example.dto.PortfolioProgressDTO;
import com.example.dto.PortfolioRecommendationDTO;
import com.example.dto.RecommendationItemDTO;
import com.example.dto.TargetAllocationDTO;
import com.example.exception.ResourceNotFoundException;
import com.example.model.AssetType;
import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.model.RiskLevel;
import com.example.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioRecommendationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DRIFT_ALERT_THRESHOLD = new BigDecimal("10.00");
    private static final BigDecimal HIGH_CONCENTRATION_THRESHOLD = new BigDecimal("40.00");
    private final PortfolioRepository portfolioRepository;

    public PortfolioRecommendationService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public PortfolioProgressDTO getPortfolioProgress(Long portfolioId) {
        Portfolio portfolio = getPortfolio(portfolioId);
        BigDecimal currentValue = calculateCurrentValue(portfolio.getItems());
        BigDecimal targetValue = normalize(portfolio.getTargetValue());
        BigDecimal remainingToTarget = targetValue.subtract(currentValue).max(BigDecimal.ZERO);

        PortfolioProgressDTO progress = new PortfolioProgressDTO();
        progress.setPortfolioId(portfolio.getId());
        progress.setPortfolioName(portfolio.getName());
        progress.setCurrency(portfolio.getCurrency());
        progress.setTargetValue(targetValue);
        progress.setCurrentValue(currentValue);
        progress.setRemainingToTarget(remainingToTarget);
        progress.setProgressPercentage(calculateProgressPercentage(currentValue, targetValue));
        progress.setStatus(buildProgressStatus(currentValue, targetValue, remainingToTarget));
        progress.setSuggestedMonthsToTarget(resolveSuggestedMonths(portfolio.getInvestmentHorizon()));
        progress.setEstimatedMonthlyContributionNeeded(
                calculateMonthlyContributionNeeded(remainingToTarget, progress.getSuggestedMonthsToTarget()));
        return progress;
    }

    public PortfolioRecommendationDTO getPortfolioRecommendations(Long portfolioId) {
        Portfolio portfolio = getPortfolio(portfolioId);
        PortfolioProgressDTO progress = getPortfolioProgress(portfolioId);
        Map<AssetType, BigDecimal> targetAllocationMap = buildTargetAllocation(portfolio);
        Map<AssetType, BigDecimal> currentAllocationMap = buildCurrentAllocation(portfolio.getItems());

        PortfolioRecommendationDTO dto = new PortfolioRecommendationDTO();
        dto.setPortfolioId(portfolio.getId());
        dto.setPortfolioName(portfolio.getName());
        dto.setCurrency(portfolio.getCurrency());
        dto.setRiskLevel(portfolio.getRiskLevel());
        dto.setInvestmentGoal(portfolio.getInvestmentGoal());
        dto.setInvestmentHorizon(portfolio.getInvestmentHorizon());
        dto.setProgress(progress);
        dto.setTargetAllocations(toAllocationList(targetAllocationMap));
        dto.setCurrentAllocations(toAllocationList(currentAllocationMap));
        dto.setAllocationDrifts(buildAllocationDrifts(targetAllocationMap, currentAllocationMap));
        dto.setRecommendations(buildRecommendations(portfolio, progress, targetAllocationMap, currentAllocationMap));
        dto.setDisclaimer("These recommendations are rule-based educational insights, not personalized financial advice.");
        return dto;
    }

    private Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
    }

    private BigDecimal calculateCurrentValue(List<PortfolioItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PortfolioItem item : items) {
            total = total.add(normalize(item.getCurrentValue()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProgressPercentage(BigDecimal currentValue, BigDecimal targetValue) {
        if (targetValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal progress = currentValue.divide(targetValue, 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);
        return progress.min(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildProgressStatus(BigDecimal currentValue, BigDecimal targetValue, BigDecimal remainingToTarget) {
        if (targetValue.compareTo(BigDecimal.ZERO) <= 0) {
            return "No target value configured yet";
        }
        if (remainingToTarget.compareTo(BigDecimal.ZERO) == 0) {
            return "Target reached";
        }
        BigDecimal progress = calculateProgressPercentage(currentValue, targetValue);
        if (progress.compareTo(new BigDecimal("75.00")) >= 0) {
            return "On track and approaching target";
        }
        if (progress.compareTo(new BigDecimal("40.00")) >= 0) {
            return "Making steady progress";
        }
        return "Early stage - consider increasing contributions";
    }

    private Integer resolveSuggestedMonths(InvestmentHorizon horizon) {
        if (horizon == null) {
            return 36;
        }
        return switch (horizon) {
            case SHORT_TERM -> 12;
            case MEDIUM_TERM -> 36;
            case LONG_TERM -> 84;
        };
    }

    private BigDecimal calculateMonthlyContributionNeeded(BigDecimal remainingToTarget, Integer months) {
        if (remainingToTarget.compareTo(BigDecimal.ZERO) <= 0 || months == null || months <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return remainingToTarget.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private Map<AssetType, BigDecimal> buildTargetAllocation(Portfolio portfolio) {
        Map<AssetType, BigDecimal> allocation = new EnumMap<>(AssetType.class);
        RiskLevel riskLevel = portfolio.getRiskLevel();
        InvestmentGoal goal = portfolio.getInvestmentGoal();
        InvestmentHorizon horizon = portfolio.getInvestmentHorizon();

        if (riskLevel == RiskLevel.CONSERVATIVE || goal == InvestmentGoal.CAPITAL_PRESERVATION || horizon == InvestmentHorizon.SHORT_TERM) {
            allocation.put(AssetType.BOND, new BigDecimal("40.00"));
            allocation.put(AssetType.ETF, new BigDecimal("25.00"));
            allocation.put(AssetType.CASH, new BigDecimal("20.00"));
            allocation.put(AssetType.STOCK, new BigDecimal("10.00"));
            allocation.put(AssetType.CRYPTO, new BigDecimal("5.00"));
            return allocation;
        }

        if (riskLevel == RiskLevel.SPECULATIVE || goal == InvestmentGoal.SPECULATION) {
            allocation.put(AssetType.STOCK, new BigDecimal("35.00"));
            allocation.put(AssetType.ETF, new BigDecimal("15.00"));
            allocation.put(AssetType.CRYPTO, new BigDecimal("35.00"));
            allocation.put(AssetType.CASH, new BigDecimal("10.00"));
            allocation.put(AssetType.OTHER, new BigDecimal("5.00"));
            return allocation;
        }

        if (riskLevel == RiskLevel.AGGRESSIVE || goal == InvestmentGoal.GROWTH || horizon == InvestmentHorizon.LONG_TERM) {
            allocation.put(AssetType.STOCK, new BigDecimal("45.00"));
            allocation.put(AssetType.ETF, new BigDecimal("30.00"));
            allocation.put(AssetType.CRYPTO, new BigDecimal("10.00"));
            allocation.put(AssetType.BOND, new BigDecimal("10.00"));
            allocation.put(AssetType.CASH, new BigDecimal("5.00"));
            return allocation;
        }

        allocation.put(AssetType.STOCK, new BigDecimal("30.00"));
        allocation.put(AssetType.ETF, new BigDecimal("30.00"));
        allocation.put(AssetType.BOND, new BigDecimal("20.00"));
        allocation.put(AssetType.CASH, new BigDecimal("10.00"));
        allocation.put(AssetType.CRYPTO, new BigDecimal("5.00"));
        allocation.put(AssetType.MUTUAL_FUND, new BigDecimal("5.00"));
        return allocation;
    }

    private Map<AssetType, BigDecimal> buildCurrentAllocation(List<PortfolioItem> items) {
        Map<AssetType, BigDecimal> valuesByAsset = new EnumMap<>(AssetType.class);
        BigDecimal totalValue = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal itemValue = normalize(item.getCurrentValue());
            valuesByAsset.merge(item.getAssetType(), itemValue, BigDecimal::add);
            totalValue = totalValue.add(itemValue);
        }

        Map<AssetType, BigDecimal> allocationPercentages = new LinkedHashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return allocationPercentages;
        }

        for (AssetType assetType : AssetType.values()) {
            BigDecimal assetValue = valuesByAsset.getOrDefault(assetType, BigDecimal.ZERO);
            if (assetValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = assetValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP);
                allocationPercentages.put(assetType, percentage);
            }
        }
        return allocationPercentages;
    }

    private List<TargetAllocationDTO> toAllocationList(Map<AssetType, BigDecimal> allocationMap) {
        List<TargetAllocationDTO> allocations = new ArrayList<>();
        for (Map.Entry<AssetType, BigDecimal> entry : allocationMap.entrySet()) {
            allocations.add(new TargetAllocationDTO(entry.getKey().name(), entry.getValue().setScale(2, RoundingMode.HALF_UP)));
        }
        return allocations;
    }

    private List<AllocationDriftDTO> buildAllocationDrifts(Map<AssetType, BigDecimal> targetAllocation,
                                                            Map<AssetType, BigDecimal> currentAllocation) {
        List<AllocationDriftDTO> drifts = new ArrayList<>();
        List<AssetType> assetTypes = new ArrayList<>();
        assetTypes.addAll(targetAllocation.keySet());
        for (AssetType assetType : currentAllocation.keySet()) {
            if (!assetTypes.contains(assetType)) {
                assetTypes.add(assetType);
            }
        }

        for (AssetType assetType : assetTypes) {
            BigDecimal current = currentAllocation.getOrDefault(assetType, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal target = targetAllocation.getOrDefault(assetType, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal drift = current.subtract(target).setScale(2, RoundingMode.HALF_UP);
            drifts.add(new AllocationDriftDTO(assetType.name(), current, target, drift));
        }
        return drifts;
    }

    private List<RecommendationItemDTO> buildRecommendations(Portfolio portfolio,
                                                              PortfolioProgressDTO progress,
                                                              Map<AssetType, BigDecimal> targetAllocation,
                                                              Map<AssetType, BigDecimal> currentAllocation) {
        List<RecommendationItemDTO> recommendations = new ArrayList<>();

        if (portfolio.getItems().isEmpty()) {
            recommendations.add(new RecommendationItemDTO(
                    "HIGH",
                    "GET_STARTED",
                    "Build a starter allocation",
                    "This portfolio has no holdings yet. Start with diversified core exposure such as ETFs plus a mix that matches your risk level and goal."
            ));
            return recommendations;
        }

        if (portfolio.getTargetValue() == null || portfolio.getTargetValue().compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add(new RecommendationItemDTO(
                    "MEDIUM",
                    "TARGET",
                    "Set a target value",
                    "Add a target value so the app can estimate the gap to goal and a suggested monthly contribution plan."
            ));
        } else if (progress.getRemainingToTarget().compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(new RecommendationItemDTO(
                    "HIGH",
                    "TARGET",
                    "Close the gap to your target",
                    String.format("You are %s %s away from your target. At the current horizon, contributing about %s %s per month would help close the gap.",
                            progress.getRemainingToTarget().toPlainString(),
                            safeCurrency(portfolio.getCurrency()),
                            progress.getEstimatedMonthlyContributionNeeded().toPlainString(),
                            safeCurrency(portfolio.getCurrency()))
            ));
        }

        for (AllocationDriftDTO drift : buildAllocationDrifts(targetAllocation, currentAllocation)) {
            if (drift.getDriftPercentage().abs().compareTo(DRIFT_ALERT_THRESHOLD) >= 0) {
                String action = drift.getDriftPercentage().compareTo(BigDecimal.ZERO) > 0 ? "reduce" : "increase";
                recommendations.add(new RecommendationItemDTO(
                        "MEDIUM",
                        "REBALANCE",
                        "Rebalance " + drift.getAssetType(),
                        String.format("Your %s allocation is %s%% versus a target of %s%%. Consider gradually %s exposure to move closer to the target mix.",
                                drift.getAssetType(),
                                drift.getCurrentPercentage().toPlainString(),
                                drift.getTargetPercentage().toPlainString(),
                                action)
                ));
            }
        }

        BigDecimal cryptoShare = currentAllocation.getOrDefault(AssetType.CRYPTO, BigDecimal.ZERO);
        if (portfolio.getRiskLevel() == RiskLevel.CONSERVATIVE && cryptoShare.compareTo(new BigDecimal("10.00")) > 0) {
            recommendations.add(new RecommendationItemDTO(
                    "HIGH",
                    "RISK",
                    "Reduce crypto concentration",
                    "For a conservative profile, crypto exposure looks elevated. Shifting part of it toward bonds, ETFs, or cash would better align with your stated risk tolerance."
            ));
        }

        BigDecimal cashShare = currentAllocation.getOrDefault(AssetType.CASH, BigDecimal.ZERO);
        if (portfolio.getInvestmentGoal() == InvestmentGoal.GROWTH && cashShare.compareTo(new BigDecimal("20.00")) > 0) {
            recommendations.add(new RecommendationItemDTO(
                    "MEDIUM",
                    "GOAL_ALIGNMENT",
                    "Put excess cash to work",
                    "A growth-oriented portfolio is holding a relatively high cash position. Consider deploying part of that cash into diversified growth assets such as ETFs or stocks."
            ));
        }

        BigDecimal bondShare = currentAllocation.getOrDefault(AssetType.BOND, BigDecimal.ZERO);
        if (portfolio.getInvestmentHorizon() == InvestmentHorizon.SHORT_TERM && bondShare.compareTo(new BigDecimal("15.00")) < 0) {
            recommendations.add(new RecommendationItemDTO(
                    "MEDIUM",
                    "HORIZON",
                    "Add stability for a shorter horizon",
                    "A short-term horizon usually benefits from more stable assets. Consider increasing bond or cash exposure to reduce volatility."
            ));
        }

        for (Map.Entry<AssetType, BigDecimal> entry : currentAllocation.entrySet()) {
            if (entry.getValue().compareTo(HIGH_CONCENTRATION_THRESHOLD) >= 0) {
                recommendations.add(new RecommendationItemDTO(
                        "MEDIUM",
                        "DIVERSIFICATION",
                        "Lower single-asset concentration",
                        String.format("%s makes up %s%% of the portfolio, which is fairly concentrated. Diversifying across more asset types can reduce concentration risk.",
                                entry.getKey().name(),
                                entry.getValue().toPlainString())
                ));
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add(new RecommendationItemDTO(
                    "LOW",
                    "MAINTENANCE",
                    "Portfolio is broadly aligned",
                    "Your current allocation is reasonably aligned with your stated risk, goal, and horizon. Continue reviewing progress periodically and rebalance when drift grows."
            ));
        }

        return recommendations;
    }

    private String safeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

