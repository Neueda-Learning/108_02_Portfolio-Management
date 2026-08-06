package com.example.service;

import com.example.dto.AssetStatsDTO;
import com.example.dto.MarketStockDTO;
import com.example.dto.PriceHistoryDTO;
import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MarketDataServiceInterface {
    BigDecimal getCurrentPrice(String ticker);

    StockPriceDTO getStockInfo(String ticker);

    boolean isTickerSupported(String ticker);

    List<MarketStockDTO> getAvailableStocks();

    List<String> getTickersByAssetType(AssetType assetType);
    
    List<PriceHistoryDTO> getPriceHistory(String ticker, LocalDate startDate, LocalDate endDate);
    
    AssetStatsDTO getAssetStats(String ticker, String period);
}

