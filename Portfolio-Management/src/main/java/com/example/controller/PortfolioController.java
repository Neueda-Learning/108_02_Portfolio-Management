package com.example.controller;

import com.example.dto.*;
import com.example.service.PortfolioRecommendationService;
import com.example.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio Management", description = "APIs for managing portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {
    
    private final PortfolioService portfolioService;
    private final PortfolioRecommendationService portfolioRecommendationService;
    
    public PortfolioController(PortfolioService portfolioService,
                               PortfolioRecommendationService portfolioRecommendationService) {
        this.portfolioService = portfolioService;
        this.portfolioRecommendationService = portfolioRecommendationService;
    }
    
    @GetMapping
    @Operation(summary = "Get all portfolios hi", description = "Retrieve a list of all portfolios")
    public ResponseEntity<List<PortfolioDTO>> getAllPortfolios() {
        List<PortfolioDTO> portfolios = portfolioService.getAllPortfolios();
        return ResponseEntity.ok(portfolios);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID", description = "Retrieve a specific portfolio by its ID")
    public ResponseEntity<PortfolioDTO> getPortfolioById(@PathVariable Long id) {
        PortfolioDTO portfolio = portfolioService.getPortfolioById(id);
        return ResponseEntity.ok(portfolio);
    }
    
    @PostMapping
    @Operation(summary = "Create new portfolio", description = "Create a new portfolio")
    public ResponseEntity<PortfolioDTO> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioDTO created = portfolioService.createPortfolio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update portfolio", description = "Update an existing portfolio")
    public ResponseEntity<PortfolioDTO> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioDTO updated = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete portfolio", description = "Delete a portfolio by ID")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/summary")
    @Operation(summary = "Get portfolio summary", description = "Get portfolio performance summary with metrics")
    public ResponseEntity<PortfolioSummaryDTO> getPortfolioSummary(@PathVariable Long id) {
        PortfolioSummaryDTO summary = portfolioService.getPortfolioSummary(id);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get portfolio target progress", description = "Get progress toward the portfolio target value and estimated contribution needed")
    public ResponseEntity<PortfolioProgressDTO> getPortfolioProgress(@PathVariable Long id) {
        PortfolioProgressDTO progress = portfolioRecommendationService.getPortfolioProgress(id);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Get portfolio recommendations", description = "Get rule-based recommendations based on target value, risk level, goal, horizon, and allocation drift")
    public ResponseEntity<PortfolioRecommendationDTO> getPortfolioRecommendations(@PathVariable Long id) {
        PortfolioRecommendationDTO recommendations = portfolioRecommendationService.getPortfolioRecommendations(id);
        return ResponseEntity.ok(recommendations);
    }
    
    @PostMapping("/{id}/refresh-prices")
    @Operation(summary = "Refresh portfolio prices", description = "Update current prices for all items in portfolio")
    public ResponseEntity<Void> refreshPortfolioPrices(@PathVariable Long id) {
        portfolioService.refreshPortfolioPrices(id);
        return ResponseEntity.ok().build();
    }
}

