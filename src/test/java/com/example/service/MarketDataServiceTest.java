package com.example.service;

import com.example.dto.AssetStatsDTO;
import com.example.dto.MarketStockDTO;
import com.example.dto.PriceHistoryDTO;
import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataServiceTest {

    @Test
    void getCurrentPrice_returnsPriceFromAwsDirectSchema() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("AAPL", mapOf("price", "123.45"));
        MarketDataService service = new MarketDataService(restTemplate);

        BigDecimal result = service.getCurrentPrice("aapl");

        assertEquals(new BigDecimal("123.45"), result);
        assertEquals(List.of("AAPL"), restTemplate.requestedAwsTickers);
        assertTrue(restTemplate.requestedYahooTickers.isEmpty());
    }

    @Test
    void getCurrentPrice_supportsAlternateAwsPriceSchemas() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("MSFT", mapOf("currentPrice", "456.78"));
        restTemplate.awsResponsesByTicker.put("NVDA", mapOf("current_price", 789.01));
        Map<String, Object> candle = new HashMap<>();
        candle.put("price_data", mapOf("close", List.of("1.00", "2.00", "333.33")));
        restTemplate.awsResponsesByTicker.put("TSLA", candle);
        MarketDataService service = new MarketDataService(restTemplate);

        BigDecimal camel = service.getCurrentPrice("msft");
        BigDecimal snake = service.getCurrentPrice("nvda");
        BigDecimal close = service.getCurrentPrice("tsla");

        assertAll(
                () -> assertEquals(new BigDecimal("456.78"), camel),
                () -> assertEquals(new BigDecimal("789.01"), snake),
                () -> assertEquals(new BigDecimal("333.33"), close)
        );
    }

    @Test
    void getCurrentPrice_usesYahooMetaWhenAwsFailsWithErrorMessage() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("GOOGL", mapOf("message", "Error: not cached"));
        restTemplate.yahooResponsesByTicker.put("GOOGL", yahooWithMetaPrice("222.22"));
        MarketDataService service = new MarketDataService(restTemplate);

        BigDecimal result = service.getCurrentPrice("googl");

        assertEquals(new BigDecimal("222.22"), result);
        assertEquals(List.of("GOOGL"), restTemplate.requestedYahooTickers);
    }

    @Test
    void getCurrentPrice_usesYahooCloseSeriesWithCryptoTickerConversion() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("BTC", mapOf("price", "bad"));
        List<Object> closes = new ArrayList<>();
        closes.add(null);
        closes.add("70000.11");
        restTemplate.yahooResponsesByTicker.put("BTC-USD", yahooWithCloseSeries(closes));
        MarketDataService service = new MarketDataService(restTemplate);

        BigDecimal result = service.getCurrentPrice("btc");

        assertEquals(new BigDecimal("70000.11"), result);
        assertEquals(List.of("BTC-USD"), restTemplate.requestedYahooTickers);
    }

    @Test
    void getCurrentPrice_returnsGeneratedFallbackWhenAwsAndYahooFail() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsThrowingTickers.add("META");
        restTemplate.yahooThrowingTickers.add("META");
        MarketDataService service = new MarketDataService(restTemplate);

        BigDecimal result = service.getCurrentPrice("meta");

        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getStockInfo_returnsNormalizedDtoAndMetadataMap() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("AMD", mapOf("price", "111.11"));
        MarketDataService service = new MarketDataService(restTemplate);

        StockPriceDTO result = service.getStockInfo("amd");

        assertAll(
                () -> assertEquals("AMD", result.getTicker()),
                () -> assertEquals(new BigDecimal("111.11"), result.getCurrentPrice()),
                () -> assertEquals("USD", result.getCurrency()),
                () -> assertTrue(result.getTimestamp() > 0),
                () -> assertEquals("AMD", String.valueOf(result.getAdditionalData().get("ticker"))),
                () -> assertEquals("Multi-source (AWS + Yahoo Finance + Fallback)", String.valueOf(result.getAdditionalData().get("source")))
        );
    }

    @Test
    void isTickerSupported_returnsTrueWithFallbackDesign() {
        MarketDataService service = new MarketDataService(new FakeRestTemplate());

        assertTrue(service.isTickerSupported("unmapped-symbol"));
    }

    @Test
    void getAvailableStocks_returnsSortedCatalogEntries() {
        MarketDataService service = new MarketDataService(new FakeRestTemplate());

        List<MarketStockDTO> result = service.getAvailableStocks();

        List<String> tickers = result.stream().map(MarketStockDTO::getTicker).toList();
        List<String> sorted = new ArrayList<>(tickers);
        sorted.sort(String::compareTo);

        assertAll(
                () -> assertEquals(34, result.size()),
                () -> assertEquals(sorted, tickers),
                () -> assertEquals("AAPL", result.get(0).getTicker()),
                () -> assertTrue(result.stream().allMatch(stock -> stock.getCurrentPrice() != null)),
                () -> assertTrue(result.stream().allMatch(MarketStockDTO::isPriceAvailable))
        );
    }

    @Test
    void getTickersByAssetType_returnsKnownAndEmptyForNull() {
        MarketDataService service = new MarketDataService(new FakeRestTemplate());

        List<String> stocks = service.getTickersByAssetType(AssetType.STOCK);
        List<String> cash = service.getTickersByAssetType(AssetType.CASH);
        List<String> none = service.getTickersByAssetType(null);

        assertAll(
                () -> assertEquals(List.of("AAPL", "AMD", "AMZN", "DIS", "GOOGL", "INTC", "JPM", "KO", "META", "MSFT", "NFLX", "NVDA", "TSLA", "V", "WMT"), stocks),
                () -> assertEquals(List.of("EUR", "JPY", "USD"), cash),
                () -> assertEquals(List.of(), none)
        );
    }

    @Test
    void getPriceHistory_generatesChronologicalDataWithFields() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("AAPL", mapOf("price", "100.00"));
        MarketDataService service = new MarketDataService(restTemplate);

        LocalDate start = LocalDate.now().minusDays(4);
        LocalDate end = LocalDate.now();
        List<PriceHistoryDTO> history = service.getPriceHistory("AAPL", start, end);

        assertEquals(5, history.size());
        assertEquals(start, history.get(0).getDate());
        assertEquals(end, history.get(history.size() - 1).getDate());
        assertTrue(history.stream().allMatch(point -> point.getOpen() != null && point.getHigh() != null && point.getLow() != null && point.getClose() != null && point.getVolume() != null));
    }

    @Test
    void getAssetStats_populatesKnownAssetAndDefaultPeriod() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("AAPL", mapOf("price", "150.00"));
        MarketDataService service = new MarketDataService(restTemplate);

        AssetStatsDTO stats = service.getAssetStats("aapl", "unknown");

        assertAll(
                () -> assertEquals("AAPL", stats.getTicker()),
                () -> assertEquals("Apple Inc.", stats.getName()),
                () -> assertEquals(AssetType.STOCK, stats.getAssetType()),
                () -> assertNotNull(stats.getCurrentPrice()),
                () -> assertFalse(stats.getPriceHistory().isEmpty()),
                () -> assertNotNull(stats.getWeekHigh()),
                () -> assertNotNull(stats.getWeekLow()),
                () -> assertNotNull(stats.getMonthHigh()),
                () -> assertNotNull(stats.getMonthLow()),
                () -> assertNotNull(stats.getYearHigh()),
                () -> assertNotNull(stats.getYearLow()),
                () -> assertNotNull(stats.getDayHigh()),
                () -> assertNotNull(stats.getDayLow())
        );
    }

    @Test
    void getAssetStats_supportsAllNamedPeriodsAndUnknownTickerDefaults() {
        FakeRestTemplate restTemplate = new FakeRestTemplate();
        restTemplate.awsResponsesByTicker.put("CUSTOM", mapOf("price", "88.00"));
        MarketDataService service = new MarketDataService(restTemplate);

        AssetStatsDTO oneWeek = service.getAssetStats("custom", "1W");
        AssetStatsDTO oneMonth = service.getAssetStats("custom", "1M");
        AssetStatsDTO oneYear = service.getAssetStats("custom", "1Y");

        assertAll(
                () -> assertEquals("CUSTOM", oneWeek.getTicker()),
                () -> assertEquals("CUSTOM", oneWeek.getName()),
                () -> assertEquals(AssetType.STOCK, oneWeek.getAssetType()),
                () -> assertTrue(oneWeek.getPriceHistory().size() >= 7),
                () -> assertTrue(oneMonth.getPriceHistory().size() >= 28),
                () -> assertTrue(oneYear.getPriceHistory().size() >= 360)
        );
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static Map<String, Object> yahooWithMetaPrice(String price) {
        Map<String, Object> meta = mapOf("regularMarketPrice", price);
        Map<String, Object> firstResult = mapOf("meta", meta);
        Map<String, Object> chart = mapOf("result", List.of(firstResult));
        return mapOf("chart", chart);
    }

    private static Map<String, Object> yahooWithCloseSeries(List<Object> closeSeries) {
        Map<String, Object> quoteData = mapOf("close", closeSeries);
        Map<String, Object> indicators = mapOf("quote", List.of(quoteData));
        Map<String, Object> firstResult = mapOf("indicators", indicators);
        Map<String, Object> chart = mapOf("result", List.of(firstResult));
        return mapOf("chart", chart);
    }

    private static final class FakeRestTemplate extends RestTemplate {
        private final Map<String, Map<String, Object>> awsResponsesByTicker = new HashMap<>();
        private final Map<String, Map<String, Object>> yahooResponsesByTicker = new HashMap<>();
        private final Set<String> awsThrowingTickers = new HashSet<>();
        private final Set<String> yahooThrowingTickers = new HashSet<>();
        private final List<String> requestedAwsTickers = new ArrayList<>();
        private final List<String> requestedYahooTickers = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
            if (url.contains("cachedPriceData?ticker=")) {
                String ticker = url.substring(url.indexOf("ticker=") + 7);
                requestedAwsTickers.add(ticker);
                if (awsThrowingTickers.contains(ticker)) {
                    throw new RestClientException("Simulated AWS failure for " + ticker);
                }
                return (T) awsResponsesByTicker.get(ticker);
            }

            if (url.contains("/chart/")) {
                String ticker = url.substring(url.lastIndexOf('/') + 1);
                requestedYahooTickers.add(ticker);
                if (yahooThrowingTickers.contains(ticker)) {
                    throw new RestClientException("Simulated Yahoo failure for " + ticker);
                }
                return (T) yahooResponsesByTicker.get(ticker);
            }

            return null;
        }
    }
}
