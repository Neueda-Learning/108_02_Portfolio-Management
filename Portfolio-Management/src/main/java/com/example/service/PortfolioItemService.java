package com.example.service;

import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.AssetTradeResponse;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.TradeAssetRequest;
import com.example.model.AssetType;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.PortfolioRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    /**
     * Buy/increase a holding for a given asset type and symbol
     */
    @Transactional
    public AssetTradeResponse buyAsset(Long portfolioId, TradeAssetRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));

        String normalizedSymbol = request.getSymbol().toUpperCase();
        Optional<PortfolioItem> existing = portfolioItemRepository.findByPortfolioIdAndSymbol(portfolioId, normalizedSymbol);
        PortfolioItem item;

        if (existing.isPresent()) {
            item = existing.get();
            if (item.getAssetType() != request.getAssetType()) {
                throw new RuntimeException("Existing holding with symbol '" + normalizedSymbol + "' has a different asset type");
            }

            BigDecimal oldQuantity = item.getQuantity();
            BigDecimal buyQuantity = request.getQuantity();
            BigDecimal newQuantity = oldQuantity.add(buyQuantity);

            BigDecimal oldCost = item.getPurchasePrice().multiply(oldQuantity);
            BigDecimal newCost = request.getTradePrice().multiply(buyQuantity);
            BigDecimal weightedAveragePrice = oldCost.add(newCost)
                    .divide(newQuantity, 2, RoundingMode.HALF_UP);

            item.setQuantity(newQuantity);
            item.setPurchasePrice(weightedAveragePrice);
            item.setPurchaseDate(resolveTradeDate(request.getTradeDate()));
            if (request.getName() != null && !request.getName().isBlank()) {
                item.setName(request.getName());
            }
            if (request.getNotes() != null && !request.getNotes().isBlank()) {
                item.setNotes(request.getNotes());
            }
        } else {
            item = new PortfolioItem();
            item.setPortfolio(portfolio);
            item.setAssetType(request.getAssetType());
            item.setSymbol(normalizedSymbol);
            item.setName(resolveName(request));
            item.setQuantity(request.getQuantity());
            item.setPurchasePrice(request.getTradePrice());
            item.setPurchaseDate(resolveTradeDate(request.getTradeDate()));
            item.setNotes(request.getNotes());
        }

        item.setCurrentPrice(resolveCurrentPriceForAsset(item.getAssetType(), normalizedSymbol, request.getTradePrice()));

        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Bought {} units of {} in portfolio {}", request.getQuantity(), normalizedSymbol, portfolioId);

        AssetTradeResponse response = new AssetTradeResponse();
        response.setAction("BUY");
        response.setPortfolioId(portfolioId);
        response.setPortfolioItemId(saved.getId());
        response.setAssetType(saved.getAssetType());
        response.setSymbol(saved.getSymbol());
        response.setTradedQuantity(request.getQuantity());
        response.setTradePrice(request.getTradePrice());
        response.setTotalAmount(request.getTradePrice().multiply(request.getQuantity()));
        response.setRemainingQuantity(saved.getQuantity());
        response.setMessage("Buy order processed successfully");
        response.setUpdatedItem(convertToDTO(saved));
        return response;
    }

    /**
     * Sell/reduce a holding for a given asset type and symbol
     */
    @Transactional
    public AssetTradeResponse sellAsset(Long portfolioId, TradeAssetRequest request) {
        String normalizedSymbol = request.getSymbol().toUpperCase();
        PortfolioItem item = portfolioItemRepository.findByPortfolioIdAndSymbol(portfolioId, normalizedSymbol)
                .orElseThrow(() -> new RuntimeException("No holding found for symbol '" + normalizedSymbol + "' in portfolio " + portfolioId));

        if (item.getAssetType() != request.getAssetType()) {
            throw new RuntimeException("Holding asset type mismatch for symbol '" + normalizedSymbol + "'");
        }

        BigDecimal sellQuantity = request.getQuantity();
        if (item.getQuantity().compareTo(sellQuantity) < 0) {
            throw new RuntimeException("Insufficient quantity to sell. Available: " + item.getQuantity());
        }

        BigDecimal remainingQuantity = item.getQuantity().subtract(sellQuantity);
        AssetTradeResponse response = new AssetTradeResponse();
        response.setAction("SELL");
        response.setPortfolioId(portfolioId);
        response.setPortfolioItemId(item.getId());
        response.setAssetType(item.getAssetType());
        response.setSymbol(item.getSymbol());
        response.setTradedQuantity(sellQuantity);
        response.setTradePrice(request.getTradePrice());
        response.setTotalAmount(request.getTradePrice().multiply(sellQuantity));
        response.setRemainingQuantity(remainingQuantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            portfolioItemRepository.delete(item);
            response.setMessage("Sell order processed and holding closed");
            response.setUpdatedItem(null);
        } else {
            item.setQuantity(remainingQuantity);
            item.setPurchaseDate(resolveTradeDate(request.getTradeDate()));
            if (request.getNotes() != null && !request.getNotes().isBlank()) {
                item.setNotes(request.getNotes());
            }
            item.setCurrentPrice(resolveCurrentPriceForAsset(item.getAssetType(), normalizedSymbol, request.getTradePrice()));
            PortfolioItem updated = portfolioItemRepository.save(item);
            response.setMessage("Sell order processed successfully");
            response.setUpdatedItem(convertToDTO(updated));
        }

        log.info("Sold {} units of {} in portfolio {}", sellQuantity, normalizedSymbol, portfolioId);
        return response;
    }
    
    // Helper method
    private BigDecimal resolveCurrentPriceForAsset(AssetType assetType, String symbol, BigDecimal fallbackPrice) {
        if (assetType == AssetType.STOCK || assetType == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(symbol);
            if (currentPrice != null) {
                return currentPrice;
            }
        }
        return fallbackPrice;
    }

    private LocalDateTime resolveTradeDate(LocalDateTime tradeDate) {
        return tradeDate != null ? tradeDate : LocalDateTime.now();
    }

    private String resolveName(TradeAssetRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName();
        }
        return request.getSymbol().toUpperCase();
    }

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

