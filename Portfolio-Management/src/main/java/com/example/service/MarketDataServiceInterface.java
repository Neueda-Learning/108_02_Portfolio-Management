package com.example.service;

import com.example.dto.StockPriceDTO;

import java.math.BigDecimal;

public interface MarketDataServiceInterface {
    BigDecimal getCurrentPrice(String ticker);

    StockPriceDTO getStockInfo(String ticker);

    boolean isTickerSupported(String ticker);
}

