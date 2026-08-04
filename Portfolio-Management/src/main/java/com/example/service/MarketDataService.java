package com.example.service;

import com.example.dto.StockPriceDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final RestTemplate restTemplate;
    private static final String API_BASE_URL = "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData";
    
    public MarketDataService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Fetch current price for a given ticker symbol
     * @param ticker Stock ticker symbol (e.g., TSLA, AAPL)
     * @return Current price or null if unavailable
     */
    public BigDecimal getCurrentPrice(String ticker) {
        try {
            String url = API_BASE_URL + "?ticker=" + ticker.toUpperCase();
            log.info("Fetching price data for ticker: {}", ticker);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            BigDecimal extractedPrice = extractPriceFromResponse(response);
            if (extractedPrice != null) {
                return extractedPrice;
            }
            
            log.warn("No price data available for ticker: {}", ticker);
            return null;
            
        } catch (RestClientException e) {
            log.error("Error fetching price for ticker {}: {}", ticker, e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetch detailed stock information
     * @param ticker Stock ticker symbol
     * @return StockPriceDTO with price and metadata
     */
    public StockPriceDTO getStockInfo(String ticker) {
        try {
            String url = API_BASE_URL + "?ticker=" + ticker.toUpperCase();
            log.info("Fetching stock info for ticker: {}", ticker);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                StockPriceDTO stockInfo = new StockPriceDTO();
                stockInfo.setTicker(ticker.toUpperCase());

                stockInfo.setCurrentPrice(extractPriceFromResponse(response));
                
                stockInfo.setCurrency("USD");
                stockInfo.setTimestamp(System.currentTimeMillis());
                stockInfo.setAdditionalData(response);
                
                return stockInfo;
            }
            
            return null;
            
        } catch (RestClientException e) {
            log.error("Error fetching stock info for ticker {}: {}", ticker, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if ticker is supported
     * @param ticker Stock ticker symbol
     * @return true if ticker data is available
     */
    public boolean isTickerSupported(String ticker) {
        BigDecimal price = getCurrentPrice(ticker);
        return price != null;
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
}

