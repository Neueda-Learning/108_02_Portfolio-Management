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
public class PortfolioItemService implements PortfolioItemServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(PortfolioItemService.class);
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataServiceInterface marketDataService;

    public PortfolioItemService(PortfolioItemRepository portfolioItemRepository,
                                PortfolioRepository portfolioRepository,
                                MarketDataServiceInterface marketDataService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
    }

    public List<PortfolioItemDTO> getPortfolioItems(Long portfolioId) {
        return portfolioItemRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PortfolioItemDTO> getItemsByAssetType(Long portfolioId, AssetType assetType) {
        return portfolioItemRepository.findByPortfolioIdAndAssetType(portfolioId, assetType)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PortfolioItemDTO getItemById(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));
        return convertToDTO(item);
    }

    @Transactional
    public PortfolioItemDTO addItemToPortfolio(Long portfolioId, CreatePortfolioItemRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));

        PortfolioItem item = new PortfolioItem();
        item.setPortfolio(portfolio);
        item.setAssetType(request.assetType());
        item.setSymbol(request.symbol().toUpperCase());
        item.setName(request.name());
        item.setQuantity(request.quantity());
        item.setPurchasePrice(request.purchasePrice());
        item.setPurchaseDate(request.purchaseDate());
        item.setNotes(request.notes());

        if (request.assetType() == AssetType.STOCK || request.assetType() == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(request.symbol());
            item.setCurrentPrice(currentPrice != null ? currentPrice : request.purchasePrice());
        } else {
            item.setCurrentPrice(request.purchasePrice());
        }

        PortfolioItem saved = portfolioItemRepository.save(item);
        log.info("Added item {} to portfolio {}", saved.getSymbol(), portfolio.getName());

        return convertToDTO(saved);
    }

    @Transactional
    public PortfolioItemDTO updateItem(Long itemId, CreatePortfolioItemRequest request) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));

        item.setAssetType(request.assetType());
        item.setSymbol(request.symbol().toUpperCase());
        item.setName(request.name());
        item.setQuantity(request.quantity());
        item.setPurchasePrice(request.purchasePrice());
        item.setPurchaseDate(request.purchaseDate());
        item.setNotes(request.notes());

        if (request.assetType() == AssetType.STOCK || request.assetType() == AssetType.ETF) {
            BigDecimal currentPrice = marketDataService.getCurrentPrice(request.symbol());
            if (currentPrice != null) {
                item.setCurrentPrice(currentPrice);
            }
        }

        PortfolioItem updated = portfolioItemRepository.save(item);
        log.info("Updated portfolio item: {}", updated.getSymbol());

        return convertToDTO(updated);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        if (!portfolioItemRepository.existsById(itemId)) {
            throw new RuntimeException("Portfolio item not found with id: " + itemId);
        }
        portfolioItemRepository.deleteById(itemId);
        log.info("Deleted portfolio item with id: {}", itemId);
    }

    @Transactional
    public PortfolioItemDTO refreshItemPrice(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Portfolio item not found with id: " + itemId));

        if (item.getAssetType() == AssetType.STOCK || item.getAssetType() == AssetType.ETF) {
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
