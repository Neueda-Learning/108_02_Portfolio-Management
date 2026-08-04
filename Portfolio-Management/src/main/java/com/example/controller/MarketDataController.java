package com.example.controller;

import com.example.dto.StockPriceDTO;
import com.example.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/market-data")
@Tag(name = "Market Data", description = "APIs for fetching live market data")
public class MarketDataController {
    
    private final MarketDataService marketDataService;
    
    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }
    
    @GetMapping("/price/{ticker}")
    @Operation(summary = "Get current price", description = "Get current price for a ticker symbol")
    public ResponseEntity<BigDecimal> getCurrentPrice(@PathVariable String ticker) {
        BigDecimal price = marketDataService.getCurrentPrice(ticker);
        if (price != null) {
            return ResponseEntity.ok(price);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/stock/{ticker}")
    @Operation(summary = "Get stock information", description = "Get detailed stock information including price")
    public ResponseEntity<StockPriceDTO> getStockInfo(@PathVariable String ticker) {
        StockPriceDTO stockInfo = marketDataService.getStockInfo(ticker);
        if (stockInfo != null) {
            return ResponseEntity.ok(stockInfo);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/check/{ticker}")
    @Operation(summary = "Check ticker support", description = "Check if a ticker symbol is supported")
    public ResponseEntity<Boolean> isTickerSupported(@PathVariable String ticker) {
        boolean supported = marketDataService.isTickerSupported(ticker);
        return ResponseEntity.ok(supported);
    }
}

