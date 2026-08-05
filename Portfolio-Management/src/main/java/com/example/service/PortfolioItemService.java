package com.example.service;

import com.example.dto.BuyAssetRequest;
import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.SellAssetRequest;
import com.example.exception.BadRequestException;
import com.example.exception.InsufficientPortfolioQuantityException;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.AssetType;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.PortfolioRepository;
import com.example.repository.UserRepositoryInterface;
import com.example.repository.WalletTransactionRepositoryInterface;
import com.example.service.MarketDataService;
import com.example.service.PortfolioItemServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioItemService implements PortfolioItemServiceInterface {
    
    private static final String BUY_TRANSACTION = "BUY";
    private static final String SELL_TRANSACTION = "SELL";

    private static final Logger log = LoggerFactory.getLogger(PortfolioItemService.class);
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioRepository portfolioRepository;
    private final com.example.service.MarketDataService marketDataService;
    private final UserRepositoryInterface userRepository;
    private final WalletTransactionRepositoryInterface walletTransactionRepository;
    
    public PortfolioItemService(PortfolioItemRepository portfolioItemRepository,
                               PortfolioRepository portfolioRepository,
                               MarketDataService marketDataService,
                               UserRepositoryInterface userRepository,
                               WalletTransactionRepositoryInterface walletTransactionRepository) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }
    
    /**
     * Get all items in a portfolio
     */
    @Override
    public List<PortfolioItemDTO> getPortfolioItems(Long portfolioId) {
        return portfolioItemRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get items by asset type
     */
    @Override
    public List<PortfolioItemDTO> getItemsByAssetType(Long portfolioId, AssetType assetType) {
        return portfolioItemRepository.findByPortfolioIdAndAssetType(portfolioId, assetType)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get item by ID
     */
    @Override
    public PortfolioItemDTO getItemById(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found with id: " + itemId));
        return convertToDTO(item);
    }
    
    /**
     * Add item to portfolio
     */
    @Transactional
    @Override
    public PortfolioItemDTO addItemToPortfolio(Long portfolioId, CreatePortfolioItemRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
        
        PortfolioItem item = new PortfolioItem();
        item.setPortfolio(portfolio);
        item.setAssetType(request.getAssetType());
        item.setSymbol(request.getSymbol().toUpperCase());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setNotes(request.getNotes());
        
        // Fetch current price for ALL asset types (not just STOCK/ETF)
        BigDecimal currentPrice = marketDataService.getCurrentPrice(request.getSymbol());
        if (currentPrice != null) {
            item.setCurrentPrice(currentPrice);
            log.info("Fetched live price for {} ({}): ${}", request.getSymbol(), request.getAssetType(), currentPrice);
        } else {
            // Use purchase price only as absolute fallback
            item.setCurrentPrice(request.getPurchasePrice());
            log.warn("Could not fetch price for {}, using purchase price: ${}", request.getSymbol(), request.getPurchasePrice());
        }
        
        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Added item {} to portfolio {}", saved.getSymbol(), portfolio.getName());
        
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public PortfolioItemDTO buyAsset(Long userId, Long portfolioId, BuyAssetRequest request) {
        Portfolio portfolio = findPortfolioForUserOrThrow(userId, portfolioId);

        BigDecimal executionPrice = resolveBuyExecutionPrice(request.symbol(), request.assetType());
        BigDecimal totalTradeAmount = calculateTradeAmount(request.quantity(), executionPrice);
        String normalizedSymbol = request.symbol().toUpperCase();

        BigDecimal walletBefore = getWalletBalanceOrThrow(userId);
        if (walletBefore.compareTo(totalTradeAmount) < 0) {
            throw new InsufficientWalletBalanceException(userId, totalTradeAmount);
        }
        if (!userRepository.removeMoney(userId, totalTradeAmount)) {
            throw new InsufficientWalletBalanceException(userId, totalTradeAmount);
        }
        BigDecimal walletAfter = walletBefore.subtract(totalTradeAmount);
        walletTransactionRepository.save(userId, BUY_TRANSACTION, totalTradeAmount, walletBefore, walletAfter);

        PortfolioItem item = portfolioItemRepository.findByPortfolioIdAndSymbol(portfolioId, normalizedSymbol)
                .map(existing -> {
                    if (existing.getAssetType() != request.assetType()) {
                        throw new BadRequestException("Symbol " + normalizedSymbol + " already exists with a different asset type");
                    }
                    existing.setName(request.name());
                    return mergeBoughtQuantity(existing, request.quantity(), executionPrice);
                })
                .orElseGet(() -> createPurchasedItem(portfolio, request, normalizedSymbol, executionPrice));

        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Bought {} {} in portfolio {} for total {}", request.quantity(), normalizedSymbol, portfolioId, totalTradeAmount);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public PortfolioItemDTO sellAsset(Long userId, Long portfolioId, SellAssetRequest request) {
        Portfolio portfolio = findPortfolioForUserOrThrow(userId, portfolioId);

        String normalizedSymbol = request.symbol().toUpperCase();
        PortfolioItem item = portfolioItemRepository.findByPortfolioIdAndSymbol(portfolioId, normalizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found for symbol: " + normalizedSymbol));

        if (item.getQuantity().compareTo(request.quantity()) < 0) {
            throw new InsufficientPortfolioQuantityException(portfolioId, normalizedSymbol, request.quantity(), item.getQuantity());
        }

        BigDecimal executionPrice = resolveExecutionPrice(
                normalizedSymbol,
                item.getAssetType(),
                request.pricePerUnit(),
                item.getCurrentPrice(),
                item.getPurchasePrice());
        BigDecimal totalTradeAmount = calculateTradeAmount(request.quantity(), executionPrice);

        BigDecimal walletBefore = getWalletBalanceOrThrow(userId);
        if (!userRepository.addMoney(userId, totalTradeAmount)) {
            throw new IllegalStateException("Failed to credit wallet for user id " + userId);
        }
        BigDecimal walletAfter = walletBefore.add(totalTradeAmount);
        walletTransactionRepository.save(userId, SELL_TRANSACTION, totalTradeAmount, walletBefore, walletAfter);

        BigDecimal remainingQuantity = item.getQuantity().subtract(request.quantity());
        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            portfolioItemRepository.deleteById(item.getId());
            item.setQuantity(BigDecimal.ZERO);
            item.setCurrentPrice(executionPrice);
            log.info("Sold all remaining {} from portfolio {}", normalizedSymbol, portfolioId);
            return convertToDTO(item);
        }

        item.setQuantity(remainingQuantity);
        item.setCurrentPrice(executionPrice);
        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Sold {} {} in portfolio {} for total {}", request.quantity(), normalizedSymbol, portfolioId, totalTradeAmount);
        return convertToDTO(saved);
    }
    
    /**
     * Update portfolio item
     */
    @Transactional
    @Override
    public PortfolioItemDTO updateItem(Long itemId, CreatePortfolioItemRequest request) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found with id: " + itemId));
        
        item.setAssetType(request.getAssetType());
        item.setSymbol(request.getSymbol().toUpperCase());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setNotes(request.getNotes());
        
        // Update current price for ALL asset types
        BigDecimal currentPrice = marketDataService.getCurrentPrice(request.getSymbol());
        if (currentPrice != null) {
            item.setCurrentPrice(currentPrice);
            log.info("Updated price for {} to ${}", request.getSymbol(), currentPrice);
        }
        
        PortfolioItem updated = portfolioItemRepository.save(item);
        log.info("Updated portfolio item: {}", updated.getSymbol());
        
        return convertToDTO(updated);
    }
    
    /**
     * Delete portfolio item
     */
    @Transactional
    @Override
    public void deleteItem(Long itemId) {
        if (!portfolioItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Portfolio item not found with id: " + itemId);
        }
        portfolioItemRepository.deleteById(itemId);
        log.info("Deleted portfolio item with id: {}", itemId);
    }
    
    /**
     * Refresh current price for a specific item
     * Works for ALL asset types (STOCK, ETF, CRYPTO, BOND, MUTUAL_FUND, etc.)
     */
    @Transactional
    @Override
    public PortfolioItemDTO refreshItemPrice(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found with id: " + itemId));
        
        // Fetch latest price for ALL asset types
        BigDecimal currentPrice = marketDataService.getCurrentPrice(item.getSymbol());
        if (currentPrice != null) {
            item.setCurrentPrice(currentPrice);
            portfolioItemRepository.save(item);
            log.info("Refreshed price for {} ({}): ${}", item.getSymbol(), item.getAssetType(), currentPrice);
        } else {
            log.warn("Could not refresh price for {}", item.getSymbol());
        }
        
        return convertToDTO(item);
    }
    
    // Helper method
    private Portfolio findPortfolioOrThrow(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
    }

    private Portfolio findPortfolioForUserOrThrow(Long userId, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        if (!userId.equals(portfolio.getUserId())) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId + " for user id: " + userId);
        }

        return portfolio;
    }

    private BigDecimal resolveBuyExecutionPrice(String symbol, AssetType assetType) {
        // Try to fetch live market price for ALL asset types
        BigDecimal marketPrice = marketDataService.getCurrentPrice(symbol);
        if (marketPrice == null) {
            throw new BadRequestException("Unable to fetch live market price for symbol: " + symbol);
        }
        log.info("Resolved buy price for {} ({}): ${}", symbol, assetType, marketPrice);
        return marketPrice;
    }

    private BigDecimal resolveExecutionPrice(
            String symbol,
            AssetType assetType,
            BigDecimal requestedPriceOverride,
            BigDecimal storedCurrentPrice,
            BigDecimal storedPurchasePrice) {
        
        // Always try to fetch live market price first for ALL asset types
        BigDecimal marketPrice = marketDataService.getCurrentPrice(symbol);
        if (marketPrice != null) {
            log.info("Using live market price for {} sell: ${}", symbol, marketPrice);
            return marketPrice;
        }
        
        // Fallback chain
        if (isPositive(requestedPriceOverride)) {
            log.warn("Using requested sell price override for {} because live market price was unavailable: ${}", symbol, requestedPriceOverride);
            return requestedPriceOverride;
        }
        if (isPositive(storedCurrentPrice)) {
            log.warn("Using stored current price for {} because live market price was unavailable: ${}", symbol, storedCurrentPrice);
            return storedCurrentPrice;
        }
        if (isPositive(storedPurchasePrice)) {
            log.warn("Using stored purchase price for {} because live market price was unavailable: ${}", symbol, storedPurchasePrice);
            return storedPurchasePrice;
        }
        
        throw new BadRequestException("Unable to resolve execution price for symbol: " + symbol);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal getWalletBalanceOrThrow(Long userId) {
        return userRepository.getWalletBalance(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private BigDecimal calculateTradeAmount(BigDecimal quantity, BigDecimal executionPrice) {
        return quantity.multiply(executionPrice).setScale(2, RoundingMode.HALF_UP);
    }

    private PortfolioItem mergeBoughtQuantity(PortfolioItem existing, BigDecimal buyQuantity, BigDecimal executionPrice) {
        BigDecimal oldQuantity = existing.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(buyQuantity);

        BigDecimal weightedCost = existing.getPurchasePrice().multiply(oldQuantity)
                .add(executionPrice.multiply(buyQuantity));

        existing.setQuantity(newQuantity);
        existing.setPurchasePrice(weightedCost.divide(newQuantity, 2, RoundingMode.HALF_UP));
        existing.setCurrentPrice(executionPrice);
        return existing;
    }

    private PortfolioItem createPurchasedItem(Portfolio portfolio, BuyAssetRequest request, String normalizedSymbol, BigDecimal executionPrice) {
        PortfolioItem item = new PortfolioItem();
        item.setPortfolio(portfolio);
        item.setAssetType(request.assetType());
        item.setSymbol(normalizedSymbol);
        item.setName(request.name());
        item.setQuantity(request.quantity());
        item.setPurchasePrice(executionPrice);
        item.setCurrentPrice(executionPrice);
        item.setPurchaseDate(LocalDateTime.now());
        item.setNotes("Created from BUY transaction");
        return item;
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

