package com.example.service;

import com.example.dto.*;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioRepository;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.UserRepositoryInterface;
import com.example.service.MarketDataService;
import com.example.service.PortfolioServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService implements PortfolioServiceInterface {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final com.example.service.MarketDataService marketDataService;
    private final UserRepositoryInterface userRepository;
    
    public PortfolioService(PortfolioRepository portfolioRepository, 
                          PortfolioItemRepository portfolioItemRepository,
                          MarketDataService marketDataService,
                          UserRepositoryInterface userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.marketDataService = marketDataService;
        this.userRepository = userRepository;
    }
    
    /**
     * Get all portfolios
     */
    @Override
    public List<PortfolioDTO> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioDTO> getPortfoliosByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get portfolio by ID
     */
    @Override
    public PortfolioDTO getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
        return convertToDTO(portfolio);
    }
    
    /**
     * Create new portfolio
     */
    @Transactional
    @Override
    public PortfolioDTO createPortfolio(CreatePortfolioRequest request) {
        if (portfolioRepository.existsByName(request.getName())) {
            throw new BadRequestException("Portfolio with name '" + request.getName() + "' already exists");
        }
        if (request.getUserId() == null) {
            throw new BadRequestException("User id is required to create a portfolio");
        }
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
        }
        
        Portfolio portfolio = new Portfolio();
        portfolio.setUserId(request.getUserId());
        portfolio.setPortfolioNumber(portfolioRepository.getNextPortfolioNumberByUserId(request.getUserId()));
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        portfolio.setRiskLevel(request.getRiskLevel());
        portfolio.setInvestmentGoal(request.getInvestmentGoal());
        portfolio.setTargetValue(request.getTargetValue());
        portfolio.setInvestmentHorizon(request.getInvestmentHorizon());
        
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Created portfolio: {}", saved.getName());
        return convertToDTO(saved);
    }
    
    /**
     * Update portfolio
     */
    @Transactional
    @Override
    public PortfolioDTO updatePortfolio(Long id, CreatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));

        if (request.getUserId() != null) {
            if (!userRepository.existsById(request.getUserId())) {
                throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
            }
            portfolio.setUserId(request.getUserId());
        }
        
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        portfolio.setRiskLevel(request.getRiskLevel());
        portfolio.setInvestmentGoal(request.getInvestmentGoal());
        portfolio.setTargetValue(request.getTargetValue());
        portfolio.setInvestmentHorizon(request.getInvestmentHorizon());

        Portfolio updated = portfolioRepository.save(portfolio);
        log.info("Updated portfolio: {}", updated.getName());
        return convertToDTO(updated);
    }
    
    /**
     * Delete portfolio
     */
    @Transactional
    @Override
    public void deletePortfolio(Long id) {
        if (!portfolioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + id);
        }
        portfolioRepository.deleteById(id);
        log.info("Deleted portfolio with id: {}", id);
    }
    
    /**
     * Get portfolio summary with performance metrics
     */
    @Override
    public PortfolioSummaryDTO getPortfolioSummary(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
        
        List<PortfolioItem> items = portfolio.getItems();
        
        // Update current prices
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
        
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO();
        summary.setPortfolioId(portfolio.getId());
        summary.setPortfolioName(portfolio.getName());
        summary.setTotalItems(items.size());
        summary.setTotalInvestment(totalInvestment);
        summary.setCurrentValue(currentValue);
        summary.setTotalProfitLoss(totalProfitLoss);
        summary.setTotalProfitLossPercentage(totalProfitLossPercentage);
        
        return summary;
    }
    
    /**
     * Refresh current prices for all items in portfolio
     */
    @Transactional
    @Override
    public void refreshPortfolioPrices(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
        
        List<PortfolioItem> items = portfolio.getItems();
        updateItemPrices(items);
        portfolioItemRepository.saveAll(items);
        
        log.info("Refreshed prices for {} items in portfolio: {}", items.size(), portfolio.getName());
    }
    
    // Helper methods
    
    private void updateItemPrices(List<PortfolioItem> items) {
        for (PortfolioItem item : items) {
            // Fetch price for ALL asset types (STOCK, ETF, CRYPTO, BOND, MUTUAL_FUND, etc.)
            BigDecimal currentPrice = marketDataService.getCurrentPrice(item.getSymbol());
            if (currentPrice != null) {
                item.setCurrentPrice(currentPrice);
                log.debug("Updated price for {} ({}): ${}", item.getSymbol(), item.getAssetType(), currentPrice);
            } else {
                log.warn("Could not update price for {} ({})", item.getSymbol(), item.getAssetType());
            }
        }
    }
    
    private PortfolioDTO convertToDTO(Portfolio portfolio) {
        PortfolioDTO dto = new PortfolioDTO();
        dto.setId(portfolio.getId());
        dto.setUserId(portfolio.getUserId());
        dto.setPortfolioNumber(portfolio.getPortfolioNumber());
        dto.setName(portfolio.getName());
        dto.setDescription(portfolio.getDescription());
        dto.setCurrency(portfolio.getCurrency());
        dto.setRiskLevel(portfolio.getRiskLevel());
        dto.setInvestmentGoal(portfolio.getInvestmentGoal());
        dto.setTargetValue(portfolio.getTargetValue());
        dto.setInvestmentHorizon(portfolio.getInvestmentHorizon());
        dto.setCreatedAt(portfolio.getCreatedAt());
        dto.setUpdatedAt(portfolio.getUpdatedAt());
        
        List<PortfolioItemDTO> itemDTOs = portfolio.getItems()
                .stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);
        
        return dto;
    }
    
    private PortfolioItemDTO convertItemToDTO(PortfolioItem item) {
        PortfolioItemDTO dto = new PortfolioItemDTO();
        dto.setId(item.getId());
        dto.setPortfolioId(item.getPortfolio().getId());
        dto.setAssetType(item.getAssetType());
        dto.setSymbol(item.getSymbol());
        dto.setName(item.getName());
        dto.setQuantity(item.getQuantity());
        dto.setPurchasePrice(item.getPurchasePrice());
        dto.setCurrentPrice(item.getCurrentPrice());
        dto.setPurchaseDate(item.getPurchaseDate());
        dto.setNotes(item.getNotes());
        dto.setTotalInvestment(item.getTotalInvestment());
        dto.setCurrentValue(item.getCurrentValue());
        dto.setProfitLoss(item.getProfitLoss());
        dto.setProfitLossPercentage(item.getProfitLossPercentage());
        
        return dto;
    }
}

