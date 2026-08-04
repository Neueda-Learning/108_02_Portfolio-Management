package com.example.controller;

import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import com.example.service.MarketDataServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@Tag(name = "Market Data", description = "APIs for fetching live market data")
@CrossOrigin(origins = "*")
public class MarketDataController {
    
    private final MarketDataServiceInterface marketDataService;
    
    public MarketDataController(MarketDataServiceInterface marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/stocks")
    @Operation(summary = "Get tickers by asset type", description = "Provide asset type (e.g. STOCK, CRYPTO, ETF) to fetch matching ticker symbols")
    public ResponseEntity<List<String>> getAvailableStocks(@RequestParam AssetType assetType)
    {
        return ResponseEntity.ok(marketDataService.getTickersByAssetType(assetType));
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

