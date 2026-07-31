package com.example.service;

import com.example.dto.*;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioRepository;
import com.example.repository.PortfolioItemRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService implements PortfolioServiceInterface {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final MarketDataServiceInterface marketDataService;
    
    public PortfolioService(PortfolioRepository portfolioRepository, 
                          PortfolioItemRepository portfolioItemRepository,
                          MarketDataServiceInterface marketDataService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.marketDataService = marketDataService;
    }
    
    /**
     * Get all portfolios
     */
    @GetMapping("/api/portfolios")
    public List<PortfolioDTO> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get portfolio by ID
     */
    public PortfolioDTO getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + id));
        return convertToDTO(portfolio);
    }
    
    /**
     * Create new portfolio
     */
    @Transactional
    public PortfolioDTO createPortfolio(CreatePortfolioRequest request) {
        if (portfolioRepository.existsByName(request.name())) {
            throw new RuntimeException("Portfolio with name '" + request.name() + "' already exists");
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.name());
        portfolio.setDescription(request.description());

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Created portfolio: {}", saved.getName());
        return convertToDTO(saved);
    }
    
    /**
     * Update portfolio
     */
    @Transactional
    public PortfolioDTO updatePortfolio(Long id, CreatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + id));

        portfolio.setName(request.name());
        portfolio.setDescription(request.description());

        Portfolio updated = portfolioRepository.save(portfolio);
        log.info("Updated portfolio: {}", updated.getName());
        return convertToDTO(updated);
    }
    
    /**
     * Delete portfolio
     */
    @Transactional
    public void deletePortfolio(Long id) {
        if (!portfolioRepository.existsById(id)) {
            throw new RuntimeException("Portfolio not found with id: " + id);
        }
        portfolioRepository.deleteById(id);
        log.info("Deleted portfolio with id: {}", id);
    }
    
    /**
     * Get portfolio summary with performance metrics
     */
    public PortfolioSummaryDTO getPortfolioSummary(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));

        List<PortfolioItem> items = portfolio.getItems();
        updateItemPrices(items);

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            totalInvestment = totalInvestment.add(item.getTotalInvestment());
            currentValue = currentValue.add(item.getCurrentValue());
        }

        BigDecimal totalProfitLoss = currentValue.subtract(totalInvestment);
        BigDecimal totalProfitLossPercentage = BigDecimal.ZERO;

        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossPercentage = totalProfitLoss
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new PortfolioSummaryDTO(
                portfolio.getId(),
                portfolio.getName(),
                items.size(),
                totalInvestment,
                currentValue,
                totalProfitLoss,
                totalProfitLossPercentage
        );
    }
    
    /**
     * Refresh current prices for all items in portfolio
     */
    @Transactional
    public void refreshPortfolioPrices(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));

        List<PortfolioItem> items = portfolio.getItems();
        updateItemPrices(items);
        portfolioItemRepository.saveAll(items);

        log.info("Refreshed prices for {} items in portfolio: {}", items.size(), portfolio.getName());
    }
    
    // Helper methods
    
    private void updateItemPrices(List<PortfolioItem> items) {
        for (PortfolioItem item : items) {
            if (item.getAssetType().name().equals("STOCK") ||
                item.getAssetType().name().equals("ETF")) {
                BigDecimal currentPrice = marketDataService.getCurrentPrice(item.getSymbol());
                if (currentPrice != null) {
                    item.setCurrentPrice(currentPrice);
                }
            }
        }
    }

    private PortfolioDTO convertToDTO(Portfolio portfolio) {
        List<PortfolioItemDTO> itemDTOs = portfolio.getItems()
                .stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());

        return new PortfolioDTO(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getDescription(),
                portfolio.getCreatedAt(),
                portfolio.getUpdatedAt(),
                itemDTOs
        );
    }

    private PortfolioItemDTO convertItemToDTO(PortfolioItem item) {
        return new PortfolioItemDTO(
                item.getId(),
                item.getPortfolio().getId(),
                item.getAssetType(),
                item.getSymbol(),
                item.getName(),
                item.getQuantity(),
                item.getPurchasePrice(),
                item.getCurrentPrice(),
                item.getPurchaseDate(),
                item.getNotes(),
                item.getTotalInvestment(),
                item.getCurrentValue(),
                item.getProfitLoss(),
                item.getProfitLossPercentage()
        );
    }
}
