package com.example.controller;

import com.example.dto.StockPriceDTO;
import com.example.model.AssetType;
import com.example.service.MarketDataServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketDataController.class)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataServiceInterface marketDataService;

    // ── GET /api/market-data/stocks ───────────────────────────────────────────────

    @Test
    void getAvailableStocks_returns200_withTickerList() throws Exception {
        when(marketDataService.getTickersByAssetType(AssetType.STOCK))
                .thenReturn(List.of("AAPL", "MSFT", "NVDA"));

        mockMvc.perform(get("/api/market-data/stocks")
                        .param("assetType", "STOCK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder("AAPL", "MSFT", "NVDA")));
    }

    @Test
    void getAvailableStocks_returns200_withEmptyList_whenNoneFound() throws Exception {
        when(marketDataService.getTickersByAssetType(AssetType.CRYPTO))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/market-data/stocks")
                        .param("assetType", "CRYPTO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAvailableStocks_returns400_forInvalidAssetType() throws Exception {
        mockMvc.perform(get("/api/market-data/stocks")
                        .param("assetType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/market-data/price/{ticker} ───────────────────────────────────────

    @Test
    void getCurrentPrice_returns200_withPrice_whenTickerExists() throws Exception {
        when(marketDataService.getCurrentPrice("AAPL"))
                .thenReturn(new BigDecimal("189.75"));

        mockMvc.perform(get("/api/market-data/price/AAPL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("189.75"));
    }

    @Test
    void getCurrentPrice_returns404_whenServiceReturnsNull() throws Exception {
        when(marketDataService.getCurrentPrice("UNKNOWN")).thenReturn(null);

        mockMvc.perform(get("/api/market-data/price/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/market-data/stock/{ticker} ───────────────────────────────────────

    @Test
    void getStockInfo_returns200_withStockPriceDto_whenTickerExists() throws Exception {
        StockPriceDTO dto = new StockPriceDTO(
                "TSLA",
                new BigDecimal("245.30"),
                "USD",
                1722700000L,
                Map.of("exchange", "NASDAQ")
        );
        when(marketDataService.getStockInfo("TSLA")).thenReturn(dto);

        mockMvc.perform(get("/api/market-data/stock/TSLA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker",       is("TSLA")))
                .andExpect(jsonPath("$.currentPrice", is(245.30)))
                .andExpect(jsonPath("$.currency",     is("USD")))
                .andExpect(jsonPath("$.timestamp",    is(1722700000)));
    }

    @Test
    void getStockInfo_returns404_whenServiceReturnsNull() throws Exception {
        when(marketDataService.getStockInfo("GHOST")).thenReturn(null);

        mockMvc.perform(get("/api/market-data/stock/GHOST"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/market-data/check/{ticker} ───────────────────────────────────────

    @Test
    void isTickerSupported_returns200True_whenTickerIsSupported() throws Exception {
        when(marketDataService.isTickerSupported("AAPL")).thenReturn(true);

        mockMvc.perform(get("/api/market-data/check/AAPL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isTickerSupported_returns200False_whenTickerIsNotSupported() throws Exception {
        when(marketDataService.isTickerSupported("XYZ123")).thenReturn(false);

        mockMvc.perform(get("/api/market-data/check/XYZ123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
