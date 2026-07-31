package com.example.service;

import com.example.dto.StockPriceDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class MarketDataService implements MarketDataServiceInterface {
    
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
            
            if (response != null && response.containsKey("price")) {
                Object priceObj = response.get("price");
                if (priceObj instanceof Number) {
                    return new BigDecimal(priceObj.toString());
                }
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
                BigDecimal price = null;
                if (response.containsKey("price")) {
                    Object priceObj = response.get("price");
                    if (priceObj instanceof Number) {
                        price = new BigDecimal(priceObj.toString());
                    }
                }

                return new StockPriceDTO(
                        ticker.toUpperCase(),
                        price,
                        "USD",
                        System.currentTimeMillis(),
                        response
                );
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
}

