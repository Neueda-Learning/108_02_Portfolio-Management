package com.example.service;

import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.model.AssetType;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.PortfolioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioItemService {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioItemService.class);
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataService marketDataService;
    
    public PortfolioItemService(PortfolioItemRepository portfolioItemRepository,
                               PortfolioRepository portfolioRepository,
                               MarketDataService marketDataService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
    }
    
    /**
     * Get all items in a portfolio
     */
    public List<PortfolioItemDTO> getPortfolioItems(Long portfolioId) {
        return portfolioItemRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get items by asset type
     */
    public List<PortfolioItemDTO> getItemsByAssetType(Long portfolioId, AssetType assetType) {
        return portfolioItemRepository.findByPortfolioIdAndAssetType(portfolioId, assetType)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get item by ID
     */
    public PortfolioItemDTO getItemById(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));
        return convertToDTO(item);
    }
    
    /**
     * Add item to portfolio
     */
    @Transactional
    public PortfolioItemDTO addItemToPortfolio(Long portfolioId, CreatePortfolioItemRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));
        
        PortfolioItem item = new PortfolioItem();
        item.setPortfolio(portfolio);
        item.setAssetType(request.getAssetType());
        item.setSymbol(request.getSymbol().toUpperCase());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setNotes(request.getNotes());
        
        // Try to fetch current price for stocks
        if (request.getAssetType() == AssetType.STOCK || 
            request.getAssetType() == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(request.getSymbol());
            if (currentPrice != null) {
                item.setCurrentPrice(currentPrice);
            } else {
                // Use purchase price as fallback
                item.setCurrentPrice(request.getPurchasePrice());
            }
        } else {
            // For non-stock assets, use purchase price
            item.setCurrentPrice(request.getPurchasePrice());
        }
        
        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Added item {} to portfolio {}", saved.getSymbol(), portfolio.getName());
        
        return convertToDTO(saved);
    }
    
    /**
     * Update portfolio item
     */
    @Transactional
    public PortfolioItemDTO updateItem(Long itemId, CreatePortfolioItemRequest request) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));
        
        item.setAssetType(request.getAssetType());
        item.setSymbol(request.getSymbol().toUpperCase());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setNotes(request.getNotes());
        
        // Update current price
        if (request.getAssetType() == AssetType.STOCK || 
            request.getAssetType() == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(request.getSymbol());
            if (currentPrice != null) {
                item.setCurrentPrice(currentPrice);
            }
        }
        
        PortfolioItem updated = portfolioItemRepository.save(item);
        log.info("Updated portfolio item: {}", updated.getSymbol());
        
        return convertToDTO(updated);
    }
    
    /**
     * Delete portfolio item
     */
    @Transactional
    public void deleteItem(Long itemId) {
        if (!portfolioItemRepository.existsById(itemId)) {
            throw new RuntimeException("Portfolio item not found with id: " + itemId);
        }
        portfolioItemRepository.deleteById(itemId);
        log.info("Deleted portfolio item with id: {}", itemId);
    }
    
    /**
     * Refresh current price for a specific item
     */
    @Transactional
    public PortfolioItemDTO refreshItemPrice(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));
        
        if (item.getAssetType() == AssetType.STOCK || 
            item.getAssetType() == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(item.getSymbol());
            if (currentPrice != null) {
                item.setCurrentPrice(currentPrice);
                portfolioItemRepository.save(item);
                log.info("Refreshed price for {}: {}", item.getSymbol(), currentPrice);
            }
        }
        
        return convertToDTO(item);
    }
    
    // Helper method
    private PortfolioItemDTO convertToDTO(PortfolioItem item) {
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

