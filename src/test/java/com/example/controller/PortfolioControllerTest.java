package com.example.controller;

import com.example.dto.*;
import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.RiskLevel;
import com.example.service.PortfolioRecommendationService;
import com.example.service.PortfolioServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioControllerTest {

    // ── GET /api/portfolios ───────────────────────────────────────────────────────

    @Test
    void getAllPortfolios_returnsAllPortfoliosWhenNoUserId() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        PortfolioDTO dto1 = buildPortfolioDTO(1L, 1L, "Growth", "Tech");
        PortfolioDTO dto2 = buildPortfolioDTO(2L, 1L, "Income", "Dividend");
        stub.allPortfolios = List.of(dto1, dto2);

        ResponseEntity<List<PortfolioDTO>> response = controller.getAllPortfolios(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals(1, stub.getAllPortfoliosCalls);
        assertEquals(0, stub.getByUserIdCalls);
    }

    @Test
    void getAllPortfolios_filtersPortfoliosByUserId() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        PortfolioDTO dto = buildPortfolioDTO(1L, 5L, "MyPortfolio", null);
        stub.userPortfolios = List.of(dto);

        ResponseEntity<List<PortfolioDTO>> response = controller.getAllPortfolios(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(5L, response.getBody().get(0).getUserId());
        assertEquals(0, stub.getAllPortfoliosCalls);
        assertEquals(1, stub.getByUserIdCalls);
    }

    // ── GET /api/portfolios/{id} ──────────────────────────────────────────────────

    @Test
    void getPortfolioById_returnsPortfolioDetails() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        PortfolioDTO dto = buildPortfolioDTO(10L, 1L, "Alpha", "Description");
        dto.setRiskLevel(RiskLevel.AGGRESSIVE);
        stub.portfolioById = dto;

        ResponseEntity<PortfolioDTO> response = controller.getPortfolioById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
        assertEquals("Alpha", response.getBody().getName());
        assertEquals(RiskLevel.AGGRESSIVE, response.getBody().getRiskLevel());
    }

    // ── POST /api/portfolios ──────────────────────────────────────────────────────

    @Test
    void createPortfolio_returns201WithCreatedPortfolio() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        PortfolioDTO created = buildPortfolioDTO(100L, 1L, "NewPortfolio", "Test");
        created.setRiskLevel(RiskLevel.MODERATE);
        stub.createdPortfolio = created;

        CreatePortfolioRequest request = new CreatePortfolioRequest(
                1L, "NewPortfolio", "Test", "USD",
                RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("50000.00"), InvestmentHorizon.MEDIUM_TERM
        );

        ResponseEntity<PortfolioDTO> response = controller.createPortfolio(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100L, response.getBody().getId());
        assertEquals("NewPortfolio", response.getBody().getName());
    }

    // ── PUT /api/portfolios/{id} ──────────────────────────────────────────────────

    @Test
    void updatePortfolio_returns200WithUpdatedPortfolio() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        PortfolioDTO updated = buildPortfolioDTO(10L, 1L, "UpdatedName", "Updated desc");
        updated.setCurrency("EUR");
        stub.updatedPortfolio = updated;

        CreatePortfolioRequest request = new CreatePortfolioRequest(
                1L, "UpdatedName", "Updated desc", "EUR",
                RiskLevel.CONSERVATIVE, InvestmentGoal.INCOME,
                new BigDecimal("100000.00"), InvestmentHorizon.LONG_TERM
        );

        ResponseEntity<PortfolioDTO> response = controller.updatePortfolio(10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UpdatedName", response.getBody().getName());
        assertEquals("EUR", response.getBody().getCurrency());
    }

    // ── DELETE /api/portfolios/{id} ───────────────────────────────────────────────

    @Test
    void deletePortfolio_returns204NoContent() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        ResponseEntity<Void> response = controller.deletePortfolio(10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(1, stub.deletePortfolioCalls);
    }

    // ── GET /api/portfolios/{id}/summary ──────────────────────────────────────────

    @Test
    void getPortfolioSummary_returns200WithSummaryDto() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        stub.summary = new PortfolioSummaryDTO(
                10L, "MyPortfolio", 5,
                new BigDecimal("50000.00"),
                new BigDecimal("55000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("10.00")
        );

        ResponseEntity<PortfolioSummaryDTO> response = controller.getPortfolioSummary(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getPortfolioId());
        assertEquals(5, response.getBody().getTotalItems());
        assertEquals(new BigDecimal("55000.00"), response.getBody().getCurrentValue());
    }

    // ── GET /api/portfolios/{id}/progress ─────────────────────────────────────────

    @Test
    void getPortfolioProgress_returns200WithProgressDto() {
        StubRecommendationService stubRec = new StubRecommendationService();
        PortfolioController controller = new PortfolioController(new StubPortfolioService(), stubRec);

        PortfolioProgressDTO progress = new PortfolioProgressDTO();
        progress.setPortfolioId(10L);
        progress.setProgressPercentage(new BigDecimal("60.00"));
        progress.setStatus("IN_PROGRESS");
        progress.setSuggestedMonthsToTarget(20);
        stubRec.progressResult = progress;

        ResponseEntity<PortfolioProgressDTO> response = controller.getPortfolioProgress(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getPortfolioId());
        assertEquals(new BigDecimal("60.00"), response.getBody().getProgressPercentage());
        assertEquals("IN_PROGRESS", response.getBody().getStatus());
    }

    // ── GET /api/portfolios/{id}/recommendations ──────────────────────────────────

    @Test
    void getPortfolioRecommendations_returns200WithRecommendationsDto() {
        StubRecommendationService stubRec = new StubRecommendationService();
        PortfolioController controller = new PortfolioController(new StubPortfolioService(), stubRec);

        PortfolioRecommendationDTO rec = new PortfolioRecommendationDTO();
        rec.setPortfolioId(10L);
        rec.setRiskLevel(RiskLevel.MODERATE);
        rec.setDisclaimer("Not financial advice");
        stubRec.recommendationResult = rec;

        ResponseEntity<PortfolioRecommendationDTO> response = controller.getPortfolioRecommendations(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getPortfolioId());
        assertEquals(RiskLevel.MODERATE, response.getBody().getRiskLevel());
        assertEquals("Not financial advice", response.getBody().getDisclaimer());
    }

    // ── POST /api/portfolios/{id}/refresh-prices ──────────────────────────────────

    @Test
    void refreshPortfolioPrices_returns200Ok() {
        StubPortfolioService stub = new StubPortfolioService();
        PortfolioController controller = new PortfolioController(stub, new StubRecommendationService());

        ResponseEntity<Void> response = controller.refreshPortfolioPrices(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, stub.refreshPricesCalls);
    }

    // ── Private Helper ────────────────────────────────────────────────────────────

    private PortfolioDTO buildPortfolioDTO(Long id, Long userId, String name, String description) {
        PortfolioDTO dto = new PortfolioDTO();
        dto.setId(id);
        dto.setUserId(userId);
        dto.setPortfolioNumber(1L);
        dto.setName(name);
        dto.setDescription(description);
        dto.setCurrency("USD");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        dto.setItems(List.of());
        return dto;
    }

    private static final class StubPortfolioService implements PortfolioServiceInterface {
        private List<PortfolioDTO> allPortfolios = List.of();
        private List<PortfolioDTO> userPortfolios = List.of();
        private PortfolioDTO portfolioById;
        private PortfolioDTO createdPortfolio;
        private PortfolioDTO updatedPortfolio;
        private PortfolioSummaryDTO summary;
        private int getAllPortfoliosCalls;
        private int getByUserIdCalls;
        private int deletePortfolioCalls;
        private int refreshPricesCalls;
        private final AtomicLong idGenerator = new AtomicLong(1000);

        @Override
        public List<PortfolioDTO> getAllPortfolios() {
            getAllPortfoliosCalls++;
            return allPortfolios;
        }

        @Override
        public List<PortfolioDTO> getPortfoliosByUserId(Long userId) {
            getByUserIdCalls++;
            return userPortfolios;
        }

        @Override
        public PortfolioDTO getPortfolioById(Long id) {
            return portfolioById;
        }

        @Override
        public PortfolioDTO createPortfolio(CreatePortfolioRequest request) {
            if (createdPortfolio != null) {
                return createdPortfolio;
            }
            PortfolioDTO dto = new PortfolioDTO();
            dto.setId(idGenerator.incrementAndGet());
            dto.setUserId(request.getUserId());
            dto.setName(request.getName());
            dto.setDescription(request.getDescription());
            dto.setCurrency(request.getCurrency());
            dto.setItems(new ArrayList<>());
            return dto;
        }

        @Override
        public PortfolioDTO updatePortfolio(Long id, CreatePortfolioRequest request) {
            return updatedPortfolio;
        }

        @Override
        public void deletePortfolio(Long id) {
            deletePortfolioCalls++;
        }

        @Override
        public PortfolioSummaryDTO getPortfolioSummary(Long id) {
            return summary;
        }

        @Override
        public void refreshPortfolioPrices(Long id) {
            refreshPricesCalls++;
        }
    }

    private static final class StubRecommendationService extends PortfolioRecommendationService {
        private PortfolioProgressDTO progressResult;
        private PortfolioRecommendationDTO recommendationResult;

        private StubRecommendationService() {
            super(null);
        }

        @Override
        public PortfolioProgressDTO getPortfolioProgress(Long portfolioId) {
            return progressResult;
        }

        @Override
        public PortfolioRecommendationDTO getPortfolioRecommendations(Long portfolioId) {
            return recommendationResult;
        }
    }
}
