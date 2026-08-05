package com.example.controller;

import com.example.dto.BuyAssetRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.SellAssetRequest;
import com.example.exception.BadRequestException;
import com.example.exception.InsufficientPortfolioQuantityException;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.AssetType;
import com.example.service.PortfolioItemServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PortfolioItemServiceInterface portfolioItemService;

	@Test
	void buyAsset_returns200_withUpdatedItem() throws Exception {
		BuyAssetRequest request = new BuyAssetRequest(
				AssetType.STOCK,
				"AAPL",
				"Apple Inc.",
				new BigDecimal("2.5000")
		);

		PortfolioItemDTO response = buildPortfolioItemDto(101L, 10L, AssetType.STOCK, "AAPL", "Apple Inc.");
		response.setCurrentPrice(new BigDecimal("189.75"));

		when(portfolioItemService.buyAsset(eq(7L), eq(10L), any(BuyAssetRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(101)))
				.andExpect(jsonPath("$.portfolioId", is(10)))
				.andExpect(jsonPath("$.assetType", is("STOCK")))
				.andExpect(jsonPath("$.symbol", is("AAPL")))
				.andExpect(jsonPath("$.name", is("Apple Inc.")));
	}

	@Test
	void sellAsset_returns200_withUpdatedItem() throws Exception {
		SellAssetRequest request = new SellAssetRequest(
				"MSFT",
				new BigDecimal("1.2500"),
				new BigDecimal("410.00")
		);

		PortfolioItemDTO response = buildPortfolioItemDto(202L, 10L, AssetType.STOCK, "MSFT", "Microsoft");
		response.setQuantity(new BigDecimal("3.7500"));
		response.setCurrentPrice(new BigDecimal("410.00"));

		when(portfolioItemService.sellAsset(eq(7L), eq(10L), any(SellAssetRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/sell")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(202)))
				.andExpect(jsonPath("$.portfolioId", is(10)))
				.andExpect(jsonPath("$.symbol", is("MSFT")))
				.andExpect(jsonPath("$.quantity", is(3.75)));
	}

	@Test
	void buyAsset_returns400_whenValidationFails() throws Exception {
		String requestBody = """
				{
				  "assetType": null,
				  "symbol": "",
				  "name": "",
				  "quantity": 0
				}
				""";

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Validation failed")))
				.andExpect(jsonPath("$.fieldErrors.assetType", is("Asset type is required")))
				.andExpect(jsonPath("$.fieldErrors.symbol", is("Symbol is required")))
				.andExpect(jsonPath("$.fieldErrors.name", is("Name is required")))
				.andExpect(jsonPath("$.fieldErrors.quantity", is("Quantity must be greater than 0")));
	}

	@Test
	void sellAsset_returns400_whenValidationFails() throws Exception {
		String requestBody = """
				{
				  "symbol": "",
				  "quantity": 0,
				  "pricePerUnit": 0
				}
				""";

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/sell")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Validation failed")))
				.andExpect(jsonPath("$.fieldErrors.symbol", is("Symbol is required")))
				.andExpect(jsonPath("$.fieldErrors.quantity", is("Quantity must be greater than 0")))
				.andExpect(jsonPath("$.fieldErrors.pricePerUnit", is("Price per unit must be greater than 0")));
	}

	@Test
	void buyAsset_returns400_forMalformedJson() throws Exception {
		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"assetType\":\"STOCK\",\"symbol\":")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void sellAsset_returns400_forMalformedJson() throws Exception {
		mockMvc.perform(post("/api/users/7/portfolios/10/trades/sell")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"symbol\":\"MSFT\",\"quantity\":")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void buyAsset_returns400_forInvalidEnumValue() throws Exception {
		String requestBody = """
				{
				  "assetType": "NOT_A_REAL_TYPE",
				  "symbol": "AAPL",
				  "name": "Apple Inc.",
				  "quantity": 1.5
				}
				""";

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void buyAsset_returns404_whenServiceThrowsResourceNotFound() throws Exception {
		BuyAssetRequest request = new BuyAssetRequest(
				AssetType.STOCK,
				"AAPL",
				"Apple Inc.",
				new BigDecimal("1.0000")
		);

		when(portfolioItemService.buyAsset(eq(7L), eq(10L), any(BuyAssetRequest.class)))
				.thenThrow(new ResourceNotFoundException("Portfolio not found"));

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Portfolio not found")));
	}

	@Test
	void buyAsset_returns409_whenServiceThrowsInsufficientWalletBalance() throws Exception {
		BuyAssetRequest request = new BuyAssetRequest(
				AssetType.STOCK,
				"NVDA",
				"NVIDIA",
				new BigDecimal("5.0000")
		);

		when(portfolioItemService.buyAsset(eq(7L), eq(10L), any(BuyAssetRequest.class)))
				.thenThrow(new InsufficientWalletBalanceException(7L, new BigDecimal("2500.00")));

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", is("Insufficient wallet balance for user id 7 to withdraw amount 2500.00")));
	}

	@Test
	void sellAsset_returns409_whenServiceThrowsInsufficientPortfolioQuantity() throws Exception {
		SellAssetRequest request = new SellAssetRequest(
				"MSFT",
				new BigDecimal("10.0000"),
				new BigDecimal("410.00")
		);

		when(portfolioItemService.sellAsset(eq(7L), eq(10L), any(SellAssetRequest.class)))
				.thenThrow(new InsufficientPortfolioQuantityException(10L, "MSFT", new BigDecimal("10.0000"), new BigDecimal("2.0000")));

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/sell")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", is("Insufficient quantity for symbol MSFT in portfolio id 10. Requested: 10.0000, available: 2.0000")));
	}

	@Test
	void buyAsset_returns400_whenServiceThrowsBadRequest() throws Exception {
		BuyAssetRequest request = new BuyAssetRequest(
				AssetType.CRYPTO,
				"BTC",
				"Bitcoin",
				new BigDecimal("0.5000")
		);

		when(portfolioItemService.buyAsset(eq(7L), eq(10L), any(BuyAssetRequest.class)))
				.thenThrow(new BadRequestException("Invalid trade configuration"));

		mockMvc.perform(post("/api/users/7/portfolios/10/trades/buy")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Invalid trade configuration")));
	}

	private PortfolioItemDTO buildPortfolioItemDto(Long id, Long portfolioId, AssetType assetType, String symbol, String name) {
		PortfolioItemDTO dto = new PortfolioItemDTO();
		dto.setId(id);
		dto.setPortfolioId(portfolioId);
		dto.setAssetType(assetType);
		dto.setSymbol(symbol);
		dto.setName(name);
		dto.setQuantity(new BigDecimal("2.5000"));
		dto.setPurchasePrice(new BigDecimal("150.00"));
		dto.setCurrentPrice(new BigDecimal("175.25"));
		return dto;
	}
}
