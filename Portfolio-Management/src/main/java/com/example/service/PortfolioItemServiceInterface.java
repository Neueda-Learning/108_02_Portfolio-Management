package com.example.service;

import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.model.AssetType;

import java.util.List;

public interface PortfolioItemServiceInterface {
    List<PortfolioItemDTO> getPortfolioItems(Long portfolioId);

    List<PortfolioItemDTO> getItemsByAssetType(Long portfolioId, AssetType assetType);

    PortfolioItemDTO getItemById(Long itemId);

    PortfolioItemDTO addItemToPortfolio(Long portfolioId, CreatePortfolioItemRequest request);

    PortfolioItemDTO updateItem(Long itemId, CreatePortfolioItemRequest request);

    void deleteItem(Long itemId);

    PortfolioItemDTO refreshItemPrice(Long itemId);
}

