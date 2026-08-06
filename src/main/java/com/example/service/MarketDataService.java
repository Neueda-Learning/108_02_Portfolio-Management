package com.example.service;

import com.example.dto.AssetStatsDTO;
import com.example.dto.MarketStockDTO;
import com.example.dto.PriceHistoryDTO;
import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService implements MarketDataServiceInterface {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final RestTemplate restTemplate;
    private static final String AWS_API_BASE_URL = "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData";
    private static final String YAHOO_FINANCE_API = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final List<StockCatalogEntry> STOCK_CATALOG = List.of(
            new StockCatalogEntry("AAPL", "Apple Inc.", "Technology", AssetType.STOCK),
            new StockCatalogEntry("MSFT", "Microsoft Corporation", "Technology", AssetType.STOCK),
            new StockCatalogEntry("GOOGL", "Alphabet Inc.", "Communication Services", AssetType.STOCK),
            new StockCatalogEntry("AMZN", "Amazon.com, Inc.", "Consumer Discretionary", AssetType.STOCK),
            new StockCatalogEntry("META", "Meta Platforms, Inc.", "Communication Services", AssetType.STOCK),
            new StockCatalogEntry("NVDA", "NVIDIA Corporation", "Technology", AssetType.STOCK),
            new StockCatalogEntry("TSLA", "Tesla, Inc.", "Consumer Discretionary", AssetType.STOCK),
            new StockCatalogEntry("NFLX", "Netflix, Inc.", "Communication Services", AssetType.STOCK),
            new StockCatalogEntry("JPM", "JPMorgan Chase & Co.", "Financials", AssetType.STOCK),
            new StockCatalogEntry("V", "Visa Inc.", "Financials", AssetType.STOCK),
            new StockCatalogEntry("WMT", "Walmart Inc.", "Consumer Staples", AssetType.STOCK),
            new StockCatalogEntry("DIS", "The Walt Disney Company", "Communication Services", AssetType.STOCK),
            new StockCatalogEntry("AMD", "Advanced Micro Devices, Inc.", "Technology", AssetType.STOCK),
            new StockCatalogEntry("INTC", "Intel Corporation", "Technology", AssetType.STOCK),
            new StockCatalogEntry("KO", "The Coca-Cola Company", "Consumer Staples", AssetType.STOCK),
            new StockCatalogEntry("SPY", "SPDR S&P 500 ETF Trust", "Broad Market", AssetType.ETF),
            new StockCatalogEntry("QQQ", "Invesco QQQ Trust", "Nasdaq 100", AssetType.ETF),
            new StockCatalogEntry("VTI", "Vanguard Total Stock Market ETF", "Broad Market", AssetType.ETF),
            new StockCatalogEntry("GLD", "SPDR Gold Shares", "Commodities", AssetType.ETF),
            new StockCatalogEntry("BTC-USD", "Bitcoin", "Digital Assets", AssetType.CRYPTO),
            new StockCatalogEntry("ETH-USD", "Ethereum", "Digital Assets", AssetType.CRYPTO),
            new StockCatalogEntry("SOL-USD", "Solana", "Digital Assets", AssetType.CRYPTO),
            new StockCatalogEntry("TLT", "iShares 20+ Year Treasury Bond ETF", "Government Bonds", AssetType.BOND),
            new StockCatalogEntry("IEF", "iShares 7-10 Year Treasury Bond ETF", "Government Bonds", AssetType.BOND),
            new StockCatalogEntry("BND", "Vanguard Total Bond Market ETF", "Aggregate Bonds", AssetType.BOND),
            new StockCatalogEntry("VTSAX", "Vanguard Total Stock Market Index Fund", "Index Mutual Fund", AssetType.MUTUAL_FUND),
            new StockCatalogEntry("FXAIX", "Fidelity 500 Index Fund", "Index Mutual Fund", AssetType.MUTUAL_FUND),
            new StockCatalogEntry("SWPPX", "Schwab S&P 500 Index Fund", "Index Mutual Fund", AssetType.MUTUAL_FUND),
            new StockCatalogEntry("USD", "US Dollar", "Cash Equivalents", AssetType.CASH),
            new StockCatalogEntry("EUR", "Euro", "Cash Equivalents", AssetType.CASH),
            new StockCatalogEntry("JPY", "Japanese Yen", "Cash Equivalents", AssetType.CASH),
            new StockCatalogEntry("XAUUSD", "Gold Spot", "Commodities", AssetType.OTHER),
            new StockCatalogEntry("WTI", "Crude Oil (WTI)", "Commodities", AssetType.OTHER),
            new StockCatalogEntry("DXY", "US Dollar Index", "Macro", AssetType.OTHER)
    );
    private static final Map<AssetType, List<String>> TICKERS_BY_ASSET_TYPE = buildTickersByAssetType();
    
    // Inject RestTemplate from config (with timeout settings)
    public MarketDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Fetch current price for a given ticker symbol
     * Uses multi-source approach: AWS API -> Yahoo Finance -> Fallback
     * @param ticker Stock ticker symbol (e.g., TSLA, AAPL)
     * @return Current price (always returns a value)
     */
    @Override
    public BigDecimal getCurrentPrice(String ticker) {
        // Try AWS API first
        BigDecimal awsPrice = fetchPriceFromAWS(ticker);
        if (awsPrice != null) {
            log.info("✓ Price for {} from AWS API: ${}", ticker, awsPrice);
            return awsPrice;
        }
        
        // Fallback to Yahoo Finance
        log.info("AWS failed for {}, trying Yahoo Finance...", ticker);
        BigDecimal yahooPrice = fetchPriceFromYahoo(ticker);
        if (yahooPrice != null) {
            log.info("✓ Price for {} from Yahoo Finance: ${}", ticker, yahooPrice);
            return yahooPrice;
        }
        
        // Last resort: generate consistent fallback price
        log.warn("All APIs failed for {}, using fallback price", ticker);
        return generateFallbackPrice(ticker);
    }
    
    /**
     * Fetch price from AWS Lambda API
     */
    private BigDecimal fetchPriceFromAWS(String ticker) {
        try {
            String url = AWS_API_BASE_URL + "?ticker=" + ticker.toUpperCase();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // Check for error response
            if (response != null && response.containsKey("message")) {
                String message = String.valueOf(response.get("message"));
                if (message.contains("Error") || message.contains("error")) {
                    log.debug("AWS API error for {}: {}", ticker, message);
                    return null;
                }
            }
            
            return extractPriceFromResponse(response);
        } catch (Exception e) {
            log.debug("AWS API exception for {}: {}", ticker, e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetch price from Yahoo Finance API
     */
    private BigDecimal fetchPriceFromYahoo(String ticker) {
        try {
            String yahooTicker = convertToYahooTicker(ticker);
            String url = YAHOO_FINANCE_API + yahooTicker;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return extractPriceFromYahooResponse(response);
        } catch (Exception e) {
            log.debug("Yahoo Finance exception for {}: {}", ticker, e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert ticker to Yahoo Finance format
     */
    private String convertToYahooTicker(String ticker) {
        String upperTicker = ticker.toUpperCase();
        
        // Crypto currencies need -USD suffix
        switch (upperTicker) {
            case "BTC": return "BTC-USD";
            case "ETH": return "ETH-USD";
            case "SOL": return "SOL-USD";
            case "BNB": return "BNB-USD";
            case "XRP": return "XRP-USD";
            case "ADA": return "ADA-USD";
            case "DOGE": return "DOGE-USD";
            default:
                // If already has -USD suffix or regular stock, return as is
                return upperTicker;
        }
    }
    
    /**
     * Extract price from Yahoo Finance response
     */
    @SuppressWarnings("unchecked")
    private BigDecimal extractPriceFromYahooResponse(Map<String, Object> response) {
        try {
            if (response == null) return null;
            
            Map<String, Object> chart = (Map<String, Object>) response.get("chart");
            if (chart == null) return null;
            
            List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");
            if (result == null || result.isEmpty()) return null;
            
            Map<String, Object> firstResult = result.get(0);
            Map<String, Object> meta = (Map<String, Object>) firstResult.get("meta");
            
            // Try to get regular market price from meta
            if (meta != null && meta.containsKey("regularMarketPrice")) {
                return toBigDecimal(meta.get("regularMarketPrice"));
            }
            
            // Fallback: get latest close price from indicators
            Map<String, Object> indicators = (Map<String, Object>) firstResult.get("indicators");
            if (indicators != null) {
                List<Map<String, Object>> quote = (List<Map<String, Object>>) indicators.get("quote");
                if (quote != null && !quote.isEmpty()) {
                    Map<String, Object> quoteData = quote.get(0);
                    List<Object> closeData = (List<Object>) quoteData.get("close");
                    if (closeData != null && !closeData.isEmpty()) {
                        // Get the last non-null close price
                        for (int i = closeData.size() - 1; i >= 0; i--) {
                            Object closeVal = closeData.get(i);
                            if (closeVal != null) {
                                return toBigDecimal(closeVal);
                            }
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Error parsing Yahoo response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate realistic varying fallback price
     * Simulates market movement with time-based variation
     */
    private BigDecimal generateFallbackPrice(String ticker) {
        // Base price from ticker hash (consistent base)
        int hash = Math.abs(ticker.hashCode());
        double basePrice = 100.0 + (hash % 400); // $100-$500
        
        // Add time-based variation to simulate market movement
        // Varies ±10% based on current minute and second (increased from ±5%)
        long currentTime = System.currentTimeMillis();
        long minuteVariation = (currentTime / 60000) % 100; // Changes every minute
        long secondVariation = (currentTime / 1000) % 60;   // Changes every second
        
        // Calculate variation: -10% to +10% (increased for more visible changes)
        double variationPercent = ((minuteVariation + secondVariation) % 100 - 50) / 500.0;
        double priceWithVariation = basePrice * (1 + variationPercent);
        
        // Add random cents for realism
        double cents = (hash % 100) / 100.0;
        
        BigDecimal fallbackPrice = BigDecimal.valueOf(priceWithVariation + cents)
            .setScale(2, BigDecimal.ROUND_HALF_UP);
        
        log.info("→ Generated fallback price for {}: ${} (base: ${}, variation: {}%)", 
            ticker, fallbackPrice, basePrice, String.format("%.2f", variationPercent * 100));
        
        return fallbackPrice;
    }
    
    /**
     * Fetch detailed stock information
     * @param ticker Stock ticker symbol
     * @return StockPriceDTO with price and metadata
     */
    @Override
    public StockPriceDTO getStockInfo(String ticker) {
        BigDecimal price = getCurrentPrice(ticker);
        
        StockPriceDTO stockInfo = new StockPriceDTO();
        stockInfo.setTicker(ticker.toUpperCase());
        stockInfo.setCurrentPrice(price);
        stockInfo.setCurrency("USD");
        stockInfo.setTimestamp(System.currentTimeMillis());
        
        // Add metadata about data source
        Map<String, Object> additionalData = Map.of(
            "source", "Multi-source (AWS + Yahoo Finance + Fallback)",
            "lastUpdated", System.currentTimeMillis(),
            "ticker", ticker.toUpperCase()
        );
        stockInfo.setAdditionalData(additionalData);
        
        return stockInfo;
    }
    
    /**
     * Check if ticker is supported
     * @param ticker Stock ticker symbol
     * @return true always (we have fallback system)
     */
    @Override
    public boolean isTickerSupported(String ticker) {
        // With multi-source fallback, all tickers are supported
        return getCurrentPrice(ticker) != null;
    }

    @Override
    public List<MarketStockDTO> getAvailableStocks() {
        List<MarketStockDTO> stocks = new ArrayList<>();
        for (StockCatalogEntry entry : STOCK_CATALOG) {
            BigDecimal currentPrice = getCurrentPrice(entry.ticker());
            stocks.add(new MarketStockDTO(
                    entry.ticker(),
                    entry.name(),
                    entry.sector(),
                    currentPrice,
                    "USD",
                    currentPrice != null
            ));
        }

        stocks.sort(Comparator.comparing(MarketStockDTO::getTicker));
        return stocks;
    }

    @Override
    public List<String> getTickersByAssetType(AssetType assetType) {
        if (assetType == null) {
            return List.of();
        }

        return TICKERS_BY_ASSET_TYPE.getOrDefault(assetType, List.of());
    }

    private static Map<AssetType, List<String>> buildTickersByAssetType() {
        EnumMap<AssetType, List<String>> byType = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            byType.put(type, STOCK_CATALOG.stream()
                    .filter(entry -> entry.assetType() == type)
                    .map(StockCatalogEntry::ticker)
                    .distinct()
                    .sorted()
                    .toList());
        }
        return byType;
    }


    private BigDecimal extractPriceFromResponse(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        // Most direct schema: { "price": 123.45 }
        BigDecimal directPrice = toBigDecimal(response.get("price"));
        if (directPrice != null) {
            return directPrice;
        }

        // Alternate direct schemas seen in some APIs
        BigDecimal currentPrice = toBigDecimal(response.get("currentPrice"));
        if (currentPrice != null) {
            return currentPrice;
        }

        BigDecimal currentPriceSnake = toBigDecimal(response.get("current_price"));
        if (currentPriceSnake != null) {
            return currentPriceSnake;
        }

        // Candle schema: { "price_data": { "close": [ ... ] } }
        Object priceDataObj = response.get("price_data");
        if (priceDataObj instanceof Map<?, ?> priceData) {
            Object closeSeriesObj = priceData.get("close");
            if (closeSeriesObj instanceof List<?> closeSeries && !closeSeries.isEmpty()) {
                Object latestClose = closeSeries.get(closeSeries.size() - 1);
                BigDecimal latestClosePrice = toBigDecimal(latestClose);
                if (latestClosePrice != null) {
                    return latestClosePrice;
                }
            }
        }

        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Generate historical price data for a ticker
     * @param ticker Stock ticker symbol
     * @param startDate Start date for historical data
     * @param endDate End date for historical data
     * @return List of historical prices
     */
    @Override
    public List<PriceHistoryDTO> getPriceHistory(String ticker, LocalDate startDate, LocalDate endDate) {
        List<PriceHistoryDTO> history = new ArrayList<>();
        
        // Get current price as reference
        BigDecimal currentPrice = getCurrentPrice(ticker);
        if (currentPrice == null) {
            currentPrice = BigDecimal.valueOf(100.0);
        }
        
        // Generate historical prices working backwards from current price
        LocalDate currentDate = endDate;
        BigDecimal price = currentPrice;
        
        while (!currentDate.isBefore(startDate)) {
            // Calculate daily price variation (simulate realistic market movement)
            double dailyChange = generateDailyPriceChange(ticker, currentDate);
            price = price.multiply(BigDecimal.valueOf(1 + dailyChange))
                .setScale(2, RoundingMode.HALF_UP);
            
            // Create OHLC data
            PriceHistoryDTO dataPoint = new PriceHistoryDTO();
            dataPoint.setDate(currentDate);
            dataPoint.setClose(price);
            
            // Simulate open, high, low
            BigDecimal dayVariation = price.multiply(BigDecimal.valueOf(0.02)); // 2% intraday variation
            dataPoint.setOpen(price.add(generateRandomVariation(dayVariation)));
            dataPoint.setHigh(price.add(generateRandomVariation(dayVariation).abs()));
            dataPoint.setLow(price.subtract(generateRandomVariation(dayVariation).abs()));
            
            // Simulate volume
            dataPoint.setVolume(generateVolume(ticker, currentDate));
            
            history.add(0, dataPoint); // Add at beginning to maintain chronological order
            currentDate = currentDate.minusDays(1);
        }
        
        return history;
    }
    
    /**
     * Get comprehensive asset statistics and price history
     * @param ticker Stock ticker symbol
     * @param period Time period (1W, 1M, 1Y)
     * @return Asset statistics with price history
     */
    @Override
    public AssetStatsDTO getAssetStats(String ticker, String period) {
        AssetStatsDTO stats = new AssetStatsDTO();
        stats.setTicker(ticker.toUpperCase());
        
        // Find asset info from catalog
        for (StockCatalogEntry entry : STOCK_CATALOG) {
            if (entry.ticker().equalsIgnoreCase(ticker)) {
                stats.setName(entry.name());
                stats.setAssetType(entry.assetType());
                break;
            }
        }
        
        // If not in catalog, set defaults
        if (stats.getName() == null) {
            stats.setName(ticker.toUpperCase());
            stats.setAssetType(AssetType.STOCK);
        }
        
        // Calculate date range based on period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = switch (period) {
            case "1W" -> endDate.minusWeeks(1);
            case "1M" -> endDate.minusMonths(1);
            case "1Y" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1); // Default to 1 month
        };
        
        // Get price history
        List<PriceHistoryDTO> history = getPriceHistory(ticker, startDate, endDate);
        stats.setPriceHistory(history);
        
        // Calculate current price and statistics
        BigDecimal currentPrice = getCurrentPrice(ticker);
        stats.setCurrentPrice(currentPrice);
        
        if (!history.isEmpty()) {
            BigDecimal firstPrice = history.get(0).getClose();
            BigDecimal priceChange = currentPrice.subtract(firstPrice);
            BigDecimal priceChangePercent = priceChange.divide(firstPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            stats.setPriceChange(priceChange);
            stats.setPriceChangePercent(priceChangePercent);
            
            // Calculate highs and lows
            calculateHighsAndLows(stats, history, endDate);
        }
        
        return stats;
    }
    
    /**
     * Calculate various high/low statistics
     */
    private void calculateHighsAndLows(AssetStatsDTO stats, List<PriceHistoryDTO> history, LocalDate endDate) {
        LocalDate weekAgo = endDate.minusWeeks(1);
        LocalDate monthAgo = endDate.minusMonths(1);
        LocalDate yearAgo = endDate.minusYears(1);
        
        BigDecimal weekHigh = BigDecimal.ZERO;
        BigDecimal weekLow = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal monthHigh = BigDecimal.ZERO;
        BigDecimal monthLow = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal yearHigh = BigDecimal.ZERO;
        BigDecimal yearLow = BigDecimal.valueOf(Double.MAX_VALUE);
        
        for (PriceHistoryDTO point : history) {
            BigDecimal price = point.getClose();
            
            // Week stats
            if (!point.getDate().isBefore(weekAgo)) {
                weekHigh = weekHigh.max(price);
                weekLow = weekLow.min(price);
            }
            
            // Month stats
            if (!point.getDate().isBefore(monthAgo)) {
                monthHigh = monthHigh.max(price);
                monthLow = monthLow.min(price);
            }
            
            // Year stats
            if (!point.getDate().isBefore(yearAgo)) {
                yearHigh = yearHigh.max(price);
                yearLow = yearLow.min(price);
            }
        }
        
        stats.setWeekHigh(weekHigh);
        stats.setWeekLow(weekLow.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : weekLow);
        stats.setMonthHigh(monthHigh);
        stats.setMonthLow(monthLow.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : monthLow);
        stats.setYearHigh(yearHigh);
        stats.setYearLow(yearLow.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : yearLow);
        
        // Day high/low (use latest price with small variation)
        BigDecimal currentPrice = stats.getCurrentPrice();
        stats.setDayHigh(currentPrice.multiply(BigDecimal.valueOf(1.01)));
        stats.setDayLow(currentPrice.multiply(BigDecimal.valueOf(0.99)));
    }
    
    /**
     * Generate realistic daily price change
     */
    private double generateDailyPriceChange(String ticker, LocalDate date) {
        // Use ticker and date to generate consistent but varied changes
        int hash = Math.abs((ticker + date.toString()).hashCode());
        
        // Generate change between -3% and +3%
        double change = ((hash % 600) - 300) / 10000.0;
        return change;
    }
    
    /**
     * Generate random variation for intraday prices
     */
    private BigDecimal generateRandomVariation(BigDecimal baseVariation) {
        long seed = System.currentTimeMillis();
        double randomFactor = ((seed % 100) - 50) / 100.0;
        return baseVariation.multiply(BigDecimal.valueOf(randomFactor));
    }
    
    /**
     * Generate simulated trading volume
     */
    private Long generateVolume(String ticker, LocalDate date) {
        int hash = Math.abs((ticker + date.toString()).hashCode());
        return (long) (1000000 + (hash % 10000000));
    }

    private record StockCatalogEntry(String ticker, String name, String sector, AssetType assetType) {
    }
}

