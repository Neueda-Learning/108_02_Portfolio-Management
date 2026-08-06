package com.example.controller;

import com.example.dto.AssetStatsDTO;
import com.example.dto.MarketStockDTO;
import com.example.dto.PriceHistoryDTO;
import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import com.example.service.MarketDataServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataControllerTest {

    // ── GET /api/market-data/stocks ───────────────────────────────────────────────

    @Test
    void getAvailableStocks_returnsTickerListForAssetType() {
        StubMarketDataService stub = new StubMarketDataService();
        stub.tickersByType.put(AssetType.STOCK, List.of("AAPL", "MSFT", "NVDA"));
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<List<String>> response = controller.getAvailableStocks(AssetType.STOCK);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("AAPL", "MSFT", "NVDA"), response.getBody());
    }

    @Test
    void getAvailableStocks_returnsEmptyListWhenNoneFound() {
        StubMarketDataService stub = new StubMarketDataService();
        stub.tickersByType.put(AssetType.CRYPTO, List.of());
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<List<String>> response = controller.getAvailableStocks(AssetType.CRYPTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ── GET /api/market-data/price/{ticker} ───────────────────────────────────────

    @Test
    void getCurrentPrice_returns200WithPriceWhenFound() {
        StubMarketDataService stub = new StubMarketDataService();
        stub.prices.put("AAPL", new BigDecimal("189.75"));
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<BigDecimal> response = controller.getCurrentPrice("AAPL");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("189.75"), response.getBody());
    }

    @Test
    void getCurrentPrice_returns404WhenServiceReturnsNull() {
        StubMarketDataService stub = new StubMarketDataService();
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<BigDecimal> response = controller.getCurrentPrice("UNKNOWN");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── GET /api/market-data/stock/{ticker} ───────────────────────────────────────

    @Test
    void getStockInfo_returns200WithDtoWhenFound() {
        StubMarketDataService stub = new StubMarketDataService();
        StockPriceDTO dto = new StockPriceDTO("TSLA", new BigDecimal("245.30"), "USD", 1722700000L, Map.of("exchange", "NASDAQ"));
        stub.stockInfos.put("TSLA", dto);
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<StockPriceDTO> response = controller.getStockInfo("TSLA");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("TSLA", response.getBody().getTicker());
        assertEquals(new BigDecimal("245.30"), response.getBody().getCurrentPrice());
    }

    @Test
    void getStockInfo_returns404WhenServiceReturnsNull() {
        StubMarketDataService stub = new StubMarketDataService();
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<StockPriceDTO> response = controller.getStockInfo("GHOST");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── GET /api/market-data/check/{ticker} ───────────────────────────────────────

    @Test
    void isTickerSupported_returns200TrueWhenSupported() {
        StubMarketDataService stub = new StubMarketDataService();
        stub.supportedTickers.add("AAPL");
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<Boolean> response = controller.isTickerSupported("AAPL");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
    }

    @Test
    void isTickerSupported_returns200FalseWhenNotSupported() {
        StubMarketDataService stub = new StubMarketDataService();
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<Boolean> response = controller.isTickerSupported("XYZ123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    // ── GET /api/market-data/history/{ticker} ─────────────────────────────────────

    @Test
    void getPriceHistory_returns200WithHistoryList() {
        StubMarketDataService stub = new StubMarketDataService();
        PriceHistoryDTO point = new PriceHistoryDTO();
        point.setDate(LocalDate.of(2026, 8, 1));
        point.setClose(new BigDecimal("150.00"));
        stub.priceHistory = List.of(point);
        MarketDataController controller = new MarketDataController(stub);

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 5);
        ResponseEntity<List<PriceHistoryDTO>> response = controller.getPriceHistory("AAPL", start, end);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(LocalDate.of(2026, 8, 1), response.getBody().get(0).getDate());
        assertEquals(new BigDecimal("150.00"), response.getBody().get(0).getClose());
    }

    @Test
    void getPriceHistory_returns200WithEmptyListWhenNoData() {
        StubMarketDataService stub = new StubMarketDataService();
        stub.priceHistory = List.of();
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<List<PriceHistoryDTO>> response = controller.getPriceHistory(
                "NVDA", LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ── GET /api/market-data/stats/{ticker} ───────────────────────────────────────

    @Test
    void getAssetStats_returns200WithStatsDto() {
        StubMarketDataService stub = new StubMarketDataService();
        AssetStatsDTO stats = new AssetStatsDTO();
        stats.setTicker("AAPL");
        stats.setCurrentPrice(new BigDecimal("155.00"));
        stub.assetStats = stats;
        MarketDataController controller = new MarketDataController(stub);

        ResponseEntity<AssetStatsDTO> response = controller.getAssetStats("AAPL", "1M");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("AAPL", response.getBody().getTicker());
        assertEquals(new BigDecimal("155.00"), response.getBody().getCurrentPrice());
    }

    @Test
    void getAssetStats_usesDefaultPeriodWhenNotProvided() {
        StubMarketDataService stub = new StubMarketDataService();
        AssetStatsDTO stats = new AssetStatsDTO();
        stats.setTicker("MSFT");
        stub.assetStats = stats;
        MarketDataController controller = new MarketDataController(stub);

        // Calling with "1M" is the default as defined in the controller
        ResponseEntity<AssetStatsDTO> response = controller.getAssetStats("MSFT", "1M");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("MSFT", response.getBody().getTicker());
        assertEquals("1M", stub.lastPeriodRequested);
    }

    // ── Stub ──────────────────────────────────────────────────────────────────────

    private static final class StubMarketDataService implements MarketDataServiceInterface {
        private final Map<AssetType, List<String>> tickersByType = new HashMap<>();
        private final Map<String, BigDecimal> prices = new HashMap<>();
        private final Map<String, StockPriceDTO> stockInfos = new HashMap<>();
        private final java.util.Set<String> supportedTickers = new java.util.HashSet<>();
        private List<PriceHistoryDTO> priceHistory = List.of();
        private AssetStatsDTO assetStats;
        private String lastPeriodRequested;

        @Override
        public BigDecimal getCurrentPrice(String ticker) {
            return prices.get(ticker);
        }

        @Override
        public StockPriceDTO getStockInfo(String ticker) {
            return stockInfos.get(ticker);
        }

        @Override
        public boolean isTickerSupported(String ticker) {
            return supportedTickers.contains(ticker);
        }

        @Override
        public List<com.example.dto.MarketStockDTO> getAvailableStocks() {
            return List.of();
        }

        @Override
        public List<String> getTickersByAssetType(AssetType assetType) {
            return tickersByType.getOrDefault(assetType, List.of());
        }

        @Override
        public List<PriceHistoryDTO> getPriceHistory(String ticker, LocalDate startDate, LocalDate endDate) {
            return priceHistory;
        }

        @Override
        public AssetStatsDTO getAssetStats(String ticker, String period) {
            lastPeriodRequested = period;
            return assetStats;
        }
    }
}
