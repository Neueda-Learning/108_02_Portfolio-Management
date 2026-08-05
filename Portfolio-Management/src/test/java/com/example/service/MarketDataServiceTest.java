package com.example.service;

import com.example.dto.MarketStockDTO;
import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataServiceTest {

	@Test
	void getCurrentPrice_returnsDirectPriceValue() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.responsesByTicker.put("AAPL", responseMap("price", 123.45));
		MarketDataService service = serviceWith(restTemplate);

		BigDecimal result = service.getCurrentPrice("aapl");

		assertEquals(new BigDecimal("123.45"), result);
		assertEquals(List.of("AAPL"), restTemplate.requestedTickers);
	}

	@Test
	void getCurrentPrice_returnsAlternateDirectFieldValues() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.responsesByTicker.put("MSFT", responseMap("currentPrice", "456.78"));
		restTemplate.responsesByTicker.put("NVDA", responseMap("current_price", 789.01));
		MarketDataService service = serviceWith(restTemplate);

		BigDecimal currentPrice = service.getCurrentPrice("msft");
		BigDecimal currentPriceSnake = service.getCurrentPrice("nvda");

		assertAll(
				() -> assertEquals(new BigDecimal("456.78"), currentPrice),
				() -> assertEquals(new BigDecimal("789.01"), currentPriceSnake)
		);
	}

	@Test
	void getCurrentPrice_returnsLatestCloseFromCandleSchema() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		Map<String, Object> priceData = new HashMap<>();
		priceData.put("close", List.of("101.10", 202.25, "303.30"));
		Map<String, Object> response = new HashMap<>();
		response.put("price_data", priceData);
		restTemplate.responsesByTicker.put("TSLA", response);
		MarketDataService service = serviceWith(restTemplate);

		BigDecimal result = service.getCurrentPrice("tsla");

		assertEquals(new BigDecimal("303.30"), result);
	}

	@Test
	void getCurrentPrice_returnsNullWhenPriceCannotBeResolved() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		Map<String, Object> response = new HashMap<>();
		response.put("price", "not-a-number");
		response.put("price_data", Map.of("close", List.of()));
		restTemplate.responsesByTicker.put("AMZN", response);
		MarketDataService service = serviceWith(restTemplate);

		BigDecimal result = service.getCurrentPrice("amzn");

		assertNull(result);
	}

	@Test
	void getCurrentPrice_returnsNullOnRestClientException() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.tickersThrowing.add("META");
		MarketDataService service = serviceWith(restTemplate);

		BigDecimal result = service.getCurrentPrice("meta");

		assertNull(result);
	}

	@Test
	void getStockInfo_returnsDtoWithNormalizedTickerAndMetadata() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		Map<String, Object> response = responseMap("price", "234.56");
		restTemplate.responsesByTicker.put("GOOGL", response);
		MarketDataService service = serviceWith(restTemplate);
		long before = System.currentTimeMillis();

		StockPriceDTO result = service.getStockInfo("googl");

		long after = System.currentTimeMillis();
		assertAll(
				() -> assertEquals("GOOGL", result.getTicker()),
				() -> assertEquals(new BigDecimal("234.56"), result.getCurrentPrice()),
				() -> assertEquals("USD", result.getCurrency()),
				() -> assertTrue(result.getTimestamp() >= before && result.getTimestamp() <= after),
				() -> assertSame(response, result.getAdditionalData())
		);
	}

	@Test
	void getStockInfo_returnsNullWhenResponseIsNull() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.responsesByTicker.put("IBM", null);
		MarketDataService service = serviceWith(restTemplate);

		StockPriceDTO result = service.getStockInfo("ibm");

		assertNull(result);
	}

	@Test
	void getStockInfo_returnsNullOnRestClientException() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.tickersThrowing.add("ORCL");
		MarketDataService service = serviceWith(restTemplate);

		StockPriceDTO result = service.getStockInfo("orcl");

		assertNull(result);
	}

	@Test
	void isTickerSupported_reflectsPriceAvailability() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.responsesByTicker.put("AAPL", responseMap("price", 10.25));
		restTemplate.responsesByTicker.put("XYZ", new HashMap<>());
		MarketDataService service = serviceWith(restTemplate);

		assertTrue(service.isTickerSupported("aapl"));
		assertFalse(service.isTickerSupported("xyz"));
	}

	@Test
	void getAvailableStocks_returnsSortedCatalogWithAvailabilityFlags() {
		FakeRestTemplate restTemplate = new FakeRestTemplate();
		restTemplate.responsesByTicker.put("AAPL", responseMap("price", 100));
		restTemplate.responsesByTicker.put("BTC-USD", responseMap("current_price", "65000.00"));
		restTemplate.responsesByTicker.put("QQQ", responseMap("currentPrice", "510.10"));
		MarketDataService service = serviceWith(restTemplate);

		List<MarketStockDTO> result = service.getAvailableStocks();

		List<String> tickers = result.stream().map(MarketStockDTO::getTicker).toList();
		List<String> sorted = new ArrayList<>(tickers);
		sorted.sort(String::compareTo);

		assertAll(
				() -> assertEquals(34, result.size()),
				() -> assertEquals(sorted, tickers),
				() -> assertEquals("AAPL", result.get(0).getTicker()),
				() -> assertTrue(result.stream().filter(stock -> stock.getTicker().equals("AAPL")).findFirst().orElseThrow().isPriceAvailable()),
				() -> assertEquals(new BigDecimal("65000.00"), result.stream().filter(stock -> stock.getTicker().equals("BTC-USD")).findFirst().orElseThrow().getCurrentPrice()),
				() -> assertFalse(result.stream().filter(stock -> stock.getTicker().equals("AMD")).findFirst().orElseThrow().isPriceAvailable())
		);
	}

	@Test
	void getTickersByAssetType_returnsSortedValuesAndEmptyForNull() {
		MarketDataService service = serviceWith(new FakeRestTemplate());

		List<String> stocks = service.getTickersByAssetType(AssetType.STOCK);
		List<String> cash = service.getTickersByAssetType(AssetType.CASH);
		List<String> nullType = service.getTickersByAssetType(null);

		assertAll(
				() -> assertEquals(List.of("AAPL", "AMD", "AMZN", "DIS", "GOOGL", "INTC", "JPM", "KO", "META", "MSFT", "NFLX", "NVDA", "TSLA", "V", "WMT"), stocks),
				() -> assertEquals(List.of("EUR", "JPY", "USD"), cash),
				() -> assertEquals(List.of(), nullType)
		);
	}

	@Test
	void extractPriceFromResponse_coversDirectAndCandleSchemas() {
		MarketDataService service = serviceWith(new FakeRestTemplate());

		BigDecimal direct = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", responseMap("price", 91.11));
		BigDecimal camel = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", responseMap("currentPrice", "92.22"));
		BigDecimal snake = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", responseMap("current_price", "93.33"));

		Map<String, Object> nested = new HashMap<>();
		nested.put("price_data", Map.of("close", List.of("1.00", "2.00", "94.44")));
		BigDecimal candle = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", nested);

		assertAll(
				() -> assertEquals(new BigDecimal("91.11"), direct),
				() -> assertEquals(new BigDecimal("92.22"), camel),
				() -> assertEquals(new BigDecimal("93.33"), snake),
				() -> assertEquals(new BigDecimal("94.44"), candle)
		);
	}

	@Test
	void extractPriceFromResponse_returnsNullForUnsupportedOrEmptyStructures() {
		MarketDataService service = serviceWith(new FakeRestTemplate());

		Map<String, Object> invalidDirect = responseMap("price", "abc");
		invalidDirect.put("price_data", Map.of("close", List.of()));
		Map<String, Object> unsupportedClose = new HashMap<>();
		unsupportedClose.put("price_data", Map.of("close", List.of(new Object())));

		BigDecimal nullResponse = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", (Object) null);
		BigDecimal emptyClose = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", invalidDirect);
		BigDecimal badCloseValue = ReflectionTestUtils.invokeMethod(service, "extractPriceFromResponse", unsupportedClose);

		assertAll(
				() -> assertNull(nullResponse),
				() -> assertNull(emptyClose),
				() -> assertNull(badCloseValue)
		);
	}

	@Test
	void toBigDecimal_parsesNumbersAndStringsAndRejectsInvalidValues() {
		MarketDataService service = serviceWith(new FakeRestTemplate());

		BigDecimal fromNumber = ReflectionTestUtils.invokeMethod(service, "toBigDecimal", 12.34);
		BigDecimal fromString = ReflectionTestUtils.invokeMethod(service, "toBigDecimal", "56.78");
		BigDecimal fromInvalidString = ReflectionTestUtils.invokeMethod(service, "toBigDecimal", "bad");
		BigDecimal fromOtherType = ReflectionTestUtils.invokeMethod(service, "toBigDecimal", new Object());
		BigDecimal fromNull = ReflectionTestUtils.invokeMethod(service, "toBigDecimal", new Object[]{null});

		assertAll(
				() -> assertEquals(new BigDecimal("12.34"), fromNumber),
				() -> assertEquals(new BigDecimal("56.78"), fromString),
				() -> assertNull(fromInvalidString),
				() -> assertNull(fromOtherType),
				() -> assertNull(fromNull)
		);
	}

	private static MarketDataService serviceWith(FakeRestTemplate restTemplate) {
		MarketDataService service = new MarketDataService();
		ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
		return service;
	}

	private static Map<String, Object> responseMap(String key, Object value) {
		Map<String, Object> response = new HashMap<>();
		response.put(key, value);
		return response;
	}

	private static final class FakeRestTemplate extends RestTemplate {
		private final Map<String, Map<String, Object>> responsesByTicker = new HashMap<>();
		private final Set<String> tickersThrowing = new HashSet<>();
		private final List<String> requestedTickers = new ArrayList<>();

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
			String ticker = extractTicker(url);
			requestedTickers.add(ticker);
			if (tickersThrowing.contains(ticker)) {
				throw new RestClientException("Simulated failure for " + ticker);
			}
			return (T) responsesByTicker.get(ticker);
		}

		private String extractTicker(String url) {
			int index = url.indexOf("ticker=");
			return index >= 0 ? url.substring(index + 7) : url;
		}
	}
}
