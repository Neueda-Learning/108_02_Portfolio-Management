package com.example.controller;

import com.example.dto.*;
import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.RiskLevel;
import com.example.service.PortfolioRecommendationService;
import com.example.service.PortfolioServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioServiceInterface portfolioService;

    @MockBean
    private PortfolioRecommendationService portfolioRecommendationService;

    @Test
    void constructor_wiresDependencies_andGetAllPortfoliosDelegates() {
        StubPortfolioService stubPortfolioService = new StubPortfolioService();
        StubRecommendationService stubRecommendationService = new StubRecommendationService();
        PortfolioController controller = new PortfolioController(stubPortfolioService, stubRecommendationService);

        PortfolioDTO dto = buildPortfolioDTO(77L, 9L, "Ctor", "check");
        stubPortfolioService.allPortfolios = List.of(dto);

        List<PortfolioDTO> result = controller.getAllPortfolios(null).getBody();

        org.junit.jupiter.api.Assertions.assertEquals(1, stubPortfolioService.getAllPortfoliosCalls);
        org.junit.jupiter.api.Assertions.assertEquals(77L, result.get(0).getId());
    }

    // ── GET /api/portfolios ───────────────────────────────────────────────────────

    @Test
    void getAllPortfolios_returns200_withAllPortfolios() throws Exception {
        PortfolioDTO portfolio1 = buildPortfolioDTO(1L, 1L, "Growth", "Tech stocks");
        PortfolioDTO portfolio2 = buildPortfolioDTO(2L, 1L, "Income", "Dividend stocks");
        
        when(portfolioService.getAllPortfolios())
                .thenReturn(List.of(portfolio1, portfolio2));

        mockMvc.perform(get("/api/portfolios")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Growth")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Income")));
    }

    @Test
    void getAllPortfolios_returns200_withEmptyList() throws Exception {
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPortfolios_withUserIdParam_returns200_andCallsGetByUserId() throws Exception {
        PortfolioDTO portfolio = buildPortfolioDTO(1L, 5L, "MyPortfolio", null);
        
        when(portfolioService.getPortfoliosByUserId(5L))
                .thenReturn(List.of(portfolio));

        mockMvc.perform(get("/api/portfolios")
                        .param("userId", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(5)));
    }

    // ── GET /api/portfolios/{id} ──────────────────────────────────────────────────

    @Test
    void getPortfolioById_returns200_withPortfolioDetails() throws Exception {
        PortfolioDTO portfolio = buildPortfolioDTO(10L, 1L, "Alpha", "Description");
        portfolio.setRiskLevel(RiskLevel.AGGRESSIVE);
        portfolio.setInvestmentGoal(InvestmentGoal.GROWTH);
        
        when(portfolioService.getPortfolioById(10L)).thenReturn(portfolio);

        mockMvc.perform(get("/api/portfolios/10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("Alpha")))
                .andExpect(jsonPath("$.description", is("Description")))
                .andExpect(jsonPath("$.riskLevel", is("AGGRESSIVE")))
                .andExpect(jsonPath("$.investmentGoal", is("GROWTH")));
    }

    // ── POST /api/portfolios ──────────────────────────────────────────────────────

    @Test
    void createPortfolio_returns201_withCreatedPortfolio() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                1L, "NewPortfolio", "Test portfolio", "USD",
                RiskLevel.MODERATE, InvestmentGoal.BALANCED,
                new BigDecimal("50000.00"), InvestmentHorizon.MEDIUM_TERM
        );
        
        PortfolioDTO created = buildPortfolioDTO(100L, 1L, "NewPortfolio", "Test portfolio");
        created.setRiskLevel(RiskLevel.MODERATE);
        
        when(portfolioService.createPortfolio(any(CreatePortfolioRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is("NewPortfolio")));
    }

    @Test
    void createPortfolio_returns400_whenNameIsBlank() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setUserId(1L);
        request.setName("");  // Blank name
        request.setCurrency("USD");

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name", containsString("Portfolio name")));
    }

    @Test
    void createPortfolio_returns400_whenNameIsTooLong() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setUserId(1L);
        request.setName("x".repeat(101));  // Too long
        request.setCurrency("USD");

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPortfolio_returns400_whenCurrencyInvalid() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setUserId(1L);
        request.setName("Test");
        request.setCurrency("INVALID");  // Invalid currency format

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPortfolio_returns400_whenTargetValueIsZeroOrNegative() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setUserId(1L);
        request.setName("Test");
        request.setCurrency("USD");
        request.setTargetValue(BigDecimal.ZERO);  // Invalid

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/portfolios/{id} ──────────────────────────────────────────────────

    @Test
    void updatePortfolio_returns200_withUpdatedPortfolio() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                1L, "UpdatedName", "Updated desc", "EUR",
                RiskLevel.CONSERVATIVE, InvestmentGoal.INCOME,
                new BigDecimal("100000.00"), InvestmentHorizon.LONG_TERM
        );
        
        PortfolioDTO updated = buildPortfolioDTO(10L, 1L, "UpdatedName", "Updated desc");
        updated.setCurrency("EUR");
        
        when(portfolioService.updatePortfolio(eq(10L), any(CreatePortfolioRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/portfolios/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("UpdatedName")))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    void updatePortfolio_returns400_onInvalidRequest() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setName("");  // Blank

        mockMvc.perform(put("/api/portfolios/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/portfolios/{id} ───────────────────────────────────────────────

    @Test
    void deletePortfolio_returns204_NoContent() throws Exception {
        doNothing().when(portfolioService).deletePortfolio(10L);

        mockMvc.perform(delete("/api/portfolios/10"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/portfolios/{id}/summary ──────────────────────────────────────────

    @Test
    void getPortfolioSummary_returns200_withSummaryDto() throws Exception {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                10L, "MyPortfolio", 5,
                new BigDecimal("50000.00"),
                new BigDecimal("55000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("10.00")
        );
        
        when(portfolioService.getPortfolioSummary(10L)).thenReturn(summary);

        mockMvc.perform(get("/api/portfolios/10/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId", is(10)))
                .andExpect(jsonPath("$.portfolioName", is("MyPortfolio")))
                .andExpect(jsonPath("$.totalItems", is(5)))
                .andExpect(jsonPath("$.currentValue", is(55000.00)))
                .andExpect(jsonPath("$.totalProfitLoss", is(5000.00)));
    }

    // ── GET /api/portfolios/{id}/progress ─────────────────────────────────────────

    @Test
    void getPortfolioProgress_returns200_withProgressDto() throws Exception {
        PortfolioProgressDTO progress = new PortfolioProgressDTO();
        progress.setPortfolioId(10L);
        progress.setPortfolioName("MyPortfolio");
        progress.setCurrency("USD");
        progress.setTargetValue(new BigDecimal("100000.00"));
        progress.setCurrentValue(new BigDecimal("60000.00"));
        progress.setRemainingToTarget(new BigDecimal("40000.00"));
        progress.setProgressPercentage(new BigDecimal("60.00"));
        progress.setStatus("IN_PROGRESS");
        progress.setSuggestedMonthsToTarget(20);
        progress.setEstimatedMonthlyContributionNeeded(new BigDecimal("2000.00"));
        
        when(portfolioRecommendationService.getPortfolioProgress(10L))
                .thenReturn(progress);

        mockMvc.perform(get("/api/portfolios/10/progress")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId", is(10)))
                .andExpect(jsonPath("$.progressPercentage", is(60.00)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.suggestedMonthsToTarget", is(20)));
    }

    // ── GET /api/portfolios/{id}/recommendations ──────────────────────────────────

    @Test
    void getPortfolioRecommendations_returns200_withRecommendationsDto() throws Exception {
        PortfolioRecommendationDTO recommendations = new PortfolioRecommendationDTO();
        recommendations.setPortfolioId(10L);
        recommendations.setPortfolioName("MyPortfolio");
        recommendations.setCurrency("USD");
        recommendations.setRiskLevel(RiskLevel.MODERATE);
        recommendations.setInvestmentGoal(InvestmentGoal.GROWTH);
        recommendations.setInvestmentHorizon(InvestmentHorizon.LONG_TERM);
        recommendations.setTargetAllocations(List.of());
        recommendations.setCurrentAllocations(List.of());
        recommendations.setAllocationDrifts(List.of());
        recommendations.setRecommendations(List.of());
        recommendations.setDisclaimer("This is not financial advice");
        
        when(portfolioRecommendationService.getPortfolioRecommendations(10L))
                .thenReturn(recommendations);

        mockMvc.perform(get("/api/portfolios/10/recommendations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId", is(10)))
                .andExpect(jsonPath("$.portfolioName", is("MyPortfolio")))
                .andExpect(jsonPath("$.riskLevel", is("MODERATE")))
                .andExpect(jsonPath("$.investmentGoal", is("GROWTH")))
                .andExpect(jsonPath("$.disclaimer", is("This is not financial advice")));
    }

    // ── POST /api/portfolios/{id}/refresh-prices ──────────────────────────────────

    @Test
    void refreshPortfolioPrices_returns200_Ok() throws Exception {
        doNothing().when(portfolioService).refreshPortfolioPrices(10L);

        mockMvc.perform(post("/api/portfolios/10/refresh-prices"))
                .andExpect(status().isOk());
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
        private int getAllPortfoliosCalls = 0;
        private final AtomicLong idGenerator = new AtomicLong(1000);

        @Override
        public List<PortfolioDTO> getAllPortfolios() {
            getAllPortfoliosCalls++;
            return allPortfolios;
        }

        @Override
        public List<PortfolioDTO> getPortfoliosByUserId(Long userId) {
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
        }

        @Override
        public PortfolioSummaryDTO getPortfolioSummary(Long id) {
            return summary;
        }

        @Override
        public void refreshPortfolioPrices(Long id) {
        }
    }

    private static final class StubRecommendationService extends PortfolioRecommendationService {

        private StubRecommendationService() {
            super(null);
        }

        @Override
        public PortfolioProgressDTO getPortfolioProgress(Long portfolioId) {
            return null;
        }

        @Override
        public PortfolioRecommendationDTO getPortfolioRecommendations(Long portfolioId) {
            return null;
        }
    }
}
