package com.example.repository;

import com.example.model.AssetType;
import com.example.model.PortfolioItem;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepositoryInterface {

    List<PortfolioItem> findByPortfolioId(Long portfolioId);

    List<PortfolioItem> findByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType);

    Optional<PortfolioItem> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    List<PortfolioItem> findBySymbol(String symbol);

    Optional<PortfolioItem> findById(Long id);

    boolean existsById(Long id);

    PortfolioItem save(PortfolioItem item);

    List<PortfolioItem> saveAll(List<PortfolioItem> items);

    void deleteById(Long id);

    void deleteByPortfolioId(Long portfolioId);
}


