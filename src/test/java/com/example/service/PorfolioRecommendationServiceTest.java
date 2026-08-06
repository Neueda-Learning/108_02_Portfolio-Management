package com.example.service;

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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PorfolioRecommendationServiceTest {

    @Test
    void getPortfolioProgress_throwsWhenPortfolioMissing() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getPortfolioProgress(999L));

        assertEquals("Portfolio not found with id: 999", ex.getMessage());
        assertEquals(1, repository.findByIdCalls);
    }

    @Test
    void getPortfolioRecommendations_throwsWhenPortfolioMissing() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getPortfolioRecommendations(404L));

        assertEquals("Portfolio not found with id: 404", ex.getMessage());
        assertEquals(1, repository.findByIdCalls);
    }

    @Test
    void getPortfolioProgress_handlesNullTargetAndNullHorizon() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio portfolio = portfolio(1L, "Starter", "USD", RiskLevel.MODERATE, InvestmentGoal.BALANCED, null, null, List.of());
        repository.portfoliosById.put(1L, portfolio);

        PortfolioProgressDTO result = service.getPortfolioProgress(1L);

        assertAll(
                () -> assertEquals(1L, result.getPortfolioId()),
                () -> assertEquals("Starter", result.getPortfolioName()),
                () -> assertEquals("USD", result.getCurrency()),
                () -> assertEquals(new BigDecimal("0.00"), result.getCurrentValue()),
                () -> assertEquals(0, result.getTargetValue().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(0, result.getRemainingToTarget().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(new BigDecimal("0.00"), result.getProgressPercentage()),
                () -> assertEquals("No target value configured yet", result.getStatus()),
                () -> assertEquals(36, result.getSuggestedMonthsToTarget()),
                () -> assertEquals(new BigDecimal("0.00"), result.getEstimatedMonthlyContributionNeeded())
        );
    }

    @Test
    void getPortfolioProgress_capsAtHundredWhenCurrentExceedsTarget() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        List<PortfolioItem> items = List.of(item(AssetType.STOCK, "AAPL", "200.00"));
        Portfolio portfolio = portfolio(2L, "Ahead", "USD", RiskLevel.AGGRESSIVE, InvestmentGoal.GROWTH,
                new BigDecimal("100.00"), InvestmentHorizon.LONG_TERM, items);
        repository.portfoliosById.put(2L, portfolio);

        PortfolioProgressDTO result = service.getPortfolioProgress(2L);

        assertAll(
                () -> assertEquals(new BigDecimal("200.00"), result.getCurrentValue()),
                () -> assertEquals(new BigDecimal("100.00"), result.getTargetValue()),
                () -> assertEquals(BigDecimal.ZERO, result.getRemainingToTarget()),
                () -> assertEquals(new BigDecimal("100.00"), result.getProgressPercentage()),
                () -> assertEquals("Target reached", result.getStatus()),
                () -> assertEquals(84, result.getSuggestedMonthsToTarget()),
                () -> assertEquals(new BigDecimal("0.00"), result.getEstimatedMonthlyContributionNeeded())
        );
    }

    @Test
    void getPortfolioProgress_reportsOnTrackStatus() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio portfolio = portfolio(3L, "On Track", "USD", RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("100.00"), InvestmentHorizon.SHORT_TERM,
                List.of(item(AssetType.ETF, "QQQ", "80.00")));
        repository.portfoliosById.put(3L, portfolio);

        PortfolioProgressDTO result = service.getPortfolioProgress(3L);

        assertAll(
                () -> assertEquals(new BigDecimal("80.00"), result.getCurrentValue()),
                () -> assertEquals(new BigDecimal("20.00"), result.getRemainingToTarget()),
                () -> assertEquals(new BigDecimal("80.00"), result.getProgressPercentage()),
                () -> assertEquals("On track and approaching target", result.getStatus()),
                () -> assertEquals(12, result.getSuggestedMonthsToTarget()),
                () -> assertEquals(new BigDecimal("1.67"), result.getEstimatedMonthlyContributionNeeded())
        );
    }

    @Test
    void getPortfolioProgress_reportsSteadyAndEarlyStatuses() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio steady = portfolio(4L, "Steady", "USD", RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("100.00"), InvestmentHorizon.MEDIUM_TERM,
                List.of(item(AssetType.BOND, "BND", "50.00")));
        Portfolio early = portfolio(5L, "Early", "USD", RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("100.00"), InvestmentHorizon.MEDIUM_TERM,
                List.of(item(AssetType.CASH, "USD", "20.00")));
        repository.portfoliosById.put(4L, steady);
        repository.portfoliosById.put(5L, early);

        PortfolioProgressDTO steadyResult = service.getPortfolioProgress(4L);
        PortfolioProgressDTO earlyResult = service.getPortfolioProgress(5L);

        assertAll(
                () -> assertEquals("Making steady progress", steadyResult.getStatus()),
                () -> assertEquals(36, steadyResult.getSuggestedMonthsToTarget()),
                () -> assertEquals("Early stage - consider increasing contributions", earlyResult.getStatus())
        );
    }

    @Test
    void getPortfolioRecommendations_forEmptyPortfolio_returnsGetStartedOnly() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio portfolio = portfolio(6L, "Empty", "USD", RiskLevel.CONSERVATIVE, InvestmentGoal.INCOME,
                new BigDecimal("1000.00"), InvestmentHorizon.SHORT_TERM, List.of());
        repository.portfoliosById.put(6L, portfolio);

        PortfolioRecommendationDTO result = service.getPortfolioRecommendations(6L);

        assertAll(
                () -> assertEquals(6L, result.getPortfolioId()),
                () -> assertEquals("Empty", result.getPortfolioName()),
                () -> assertTrue(result.getCurrentAllocations().isEmpty()),
                () -> assertEquals(1, result.getRecommendations().size()),
                () -> assertEquals("GET_STARTED", result.getRecommendations().get(0).getCategory()),
                () -> assertEquals(
                        "These recommendations are rule-based educational insights, not personalized financial advice.",
                        result.getDisclaimer())
        );
    }

    @Test
    void getPortfolioRecommendations_buildsSpeculativeTargetAllocation() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio portfolio = portfolio(7L, "Spec", "USD", RiskLevel.SPECULATIVE, InvestmentGoal.BALANCED,
                new BigDecimal("200.00"), InvestmentHorizon.MEDIUM_TERM,
                List.of(item(AssetType.STOCK, "AAPL", "100.00")));
        repository.portfoliosById.put(7L, portfolio);

        PortfolioRecommendationDTO result = service.getPortfolioRecommendations(7L);
        Map<String, BigDecimal> target = toPercentageMap(result.getTargetAllocations());

        assertAll(
                () -> assertEquals(new BigDecimal("35.00"), target.get("STOCK")),
                () -> assertEquals(new BigDecimal("15.00"), target.get("ETF")),
                () -> assertEquals(new BigDecimal("35.00"), target.get("CRYPTO")),
                () -> assertEquals(new BigDecimal("10.00"), target.get("CASH")),
                () -> assertEquals(new BigDecimal("5.00"), target.get("OTHER"))
        );
    }

    @Test
    void getPortfolioRecommendations_buildsAggressiveTargetAllocation() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        Portfolio portfolio = portfolio(8L, "Agg", "USD", RiskLevel.AGGRESSIVE, InvestmentGoal.BALANCED,
                new BigDecimal("200.00"), InvestmentHorizon.MEDIUM_TERM,
                List.of(item(AssetType.STOCK, "AAPL", "100.00")));
        repository.portfoliosById.put(8L, portfolio);

        PortfolioRecommendationDTO result = service.getPortfolioRecommendations(8L);
        Map<String, BigDecimal> target = toPercentageMap(result.getTargetAllocations());

        assertAll(
                () -> assertEquals(new BigDecimal("45.00"), target.get("STOCK")),
                () -> assertEquals(new BigDecimal("30.00"), target.get("ETF")),
                () -> assertEquals(new BigDecimal("10.00"), target.get("CRYPTO")),
                () -> assertEquals(new BigDecimal("10.00"), target.get("BOND")),
                () -> assertEquals(new BigDecimal("5.00"), target.get("CASH"))
        );
    }

    @Test
    void getPortfolioRecommendations_addsGapRebalanceAndRiskRecommendations() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        List<PortfolioItem> items = List.of(
                item(AssetType.CRYPTO, "BTC-USD", "40.00"),
                item(AssetType.CASH, "USD", "30.00"),
                item(AssetType.STOCK, "AAPL", "20.00"),
                item(AssetType.OTHER, "DXY", "10.00")
        );

        Portfolio portfolio = portfolio(9L, "Rule Trigger", " ", RiskLevel.CONSERVATIVE, InvestmentGoal.GROWTH,
                new BigDecimal("250.00"), InvestmentHorizon.SHORT_TERM, items);
        repository.portfoliosById.put(9L, portfolio);

        PortfolioRecommendationDTO result = service.getPortfolioRecommendations(9L);
        List<RecommendationItemDTO> recommendations = result.getRecommendations();

        assertAll(
                () -> assertTrue(containsCategory(recommendations, "TARGET")),
                () -> assertTrue(containsCategory(recommendations, "REBALANCE")),
                () -> assertTrue(containsCategory(recommendations, "RISK")),
                () -> assertTrue(containsCategory(recommendations, "GOAL_ALIGNMENT")),
                () -> assertTrue(containsCategory(recommendations, "HORIZON")),
                () -> assertTrue(containsCategory(recommendations, "DIVERSIFICATION")),
                () -> assertTrue(recommendations.stream().anyMatch(r -> r.getMessage().contains("USD per month"))),
                () -> assertTrue(recommendations.stream().anyMatch(r -> r.getMessage().contains("reduce exposure"))),
                () -> assertTrue(recommendations.stream().anyMatch(r -> r.getMessage().contains("increase exposure"))),
                () -> assertTrue(result.getAllocationDrifts().stream().anyMatch(d -> d.getAssetType().equals("OTHER")))
        );
    }

    @Test
    void getPortfolioRecommendations_returnsMaintenanceWhenAligned() {
        FakePortfolioRepository repository = new FakePortfolioRepository();
        PortfolioRecommendationService service = new PortfolioRecommendationService(repository);

        List<PortfolioItem> alignedItems = List.of(
                item(AssetType.STOCK, "AAPL", "30.00"),
                item(AssetType.ETF, "QQQ", "30.00"),
                item(AssetType.BOND, "BND", "20.00"),
                item(AssetType.CASH, "USD", "10.00"),
                item(AssetType.CRYPTO, "BTC-USD", "5.00"),
                item(AssetType.MUTUAL_FUND, "VTSAX", "5.00")
        );

        Portfolio portfolio = portfolio(10L, "Aligned", "EUR", RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("100.00"), InvestmentHorizon.MEDIUM_TERM, alignedItems);
        repository.portfoliosById.put(10L, portfolio);

        PortfolioRecommendationDTO result = service.getPortfolioRecommendations(10L);

        assertAll(
                () -> assertEquals(1, result.getRecommendations().size()),
                () -> assertEquals("MAINTENANCE", result.getRecommendations().get(0).getCategory()),
                () -> assertFalse(result.getAllocationDrifts().isEmpty()),
                () -> assertEquals("EUR", result.getCurrency())
        );
    }

    private static boolean containsCategory(List<RecommendationItemDTO> recommendations, String category) {
        return recommendations.stream().anyMatch(item -> item.getCategory().equals(category));
    }

    private static Map<String, BigDecimal> toPercentageMap(List<TargetAllocationDTO> allocations) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (TargetAllocationDTO allocation : allocations) {
            map.put(allocation.getAssetType(), allocation.getPercentage());
        }
        return map;
    }

    private static Portfolio portfolio(Long id,
                                       String name,
                                       String currency,
                                       RiskLevel riskLevel,
                                       InvestmentGoal goal,
                                       BigDecimal targetValue,
                                       InvestmentHorizon horizon,
                                       List<PortfolioItem> items) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setName(name);
        portfolio.setCurrency(currency);
        portfolio.setRiskLevel(riskLevel);
        portfolio.setInvestmentGoal(goal);
        portfolio.setTargetValue(targetValue);
        portfolio.setInvestmentHorizon(horizon);
        portfolio.setItems(new ArrayList<>(items));
        return portfolio;
    }

    private static PortfolioItem item(AssetType assetType, String symbol, String value) {
        PortfolioItem item = new PortfolioItem();
        item.setAssetType(assetType);
        item.setSymbol(symbol);
        item.setQuantity(BigDecimal.ONE);
        item.setPurchasePrice(new BigDecimal(value));
        item.setCurrentPrice(new BigDecimal(value));
        return item;
    }

    private static final class FakePortfolioRepository extends PortfolioRepository {
        private final Map<Long, Portfolio> portfoliosById = new HashMap<>();
        private int findByIdCalls;

        private FakePortfolioRepository() {
            super(null, null);
        }

        @Override
        public Optional<Portfolio> findById(Long id) {
            findByIdCalls++;
            return Optional.ofNullable(portfoliosById.get(id));
        }
    }
}
