package com.example.controller;

import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.exception.BadRequestException;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioItemController.class)
class PortfolioItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PortfolioItemServiceInterface portfolioItemService;

	@Test
	void getPortfolioItems_returns200_withItems() throws Exception {
		when(portfolioItemService.getPortfolioItems(10L)).thenReturn(List.of(
				buildItemDto(1L, 10L, AssetType.STOCK, "AAPL", "Apple"),
				buildItemDto(2L, 10L, AssetType.ETF, "VTI", "Vanguard Total Stock Market ETF")
		));

		mockMvc.perform(get("/api/portfolios/10/items").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].symbol", is("AAPL")))
				.andExpect(jsonPath("$[1].assetType", is("ETF")));
	}

	@Test
	void getItemsByAssetType_returns200_withFilteredItems() throws Exception {
		when(portfolioItemService.getItemsByAssetType(10L, AssetType.CRYPTO)).thenReturn(List.of(
				buildItemDto(3L, 10L, AssetType.CRYPTO, "BTC", "Bitcoin")
		));

		mockMvc.perform(get("/api/portfolios/10/items/by-type/CRYPTO").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].assetType", is("CRYPTO")))
				.andExpect(jsonPath("$[0].symbol", is("BTC")));
	}

	@Test
	void getItemsByAssetType_returns400_forInvalidAssetTypePathVariable() throws Exception {
		mockMvc.perform(get("/api/portfolios/10/items/by-type/NOT_REAL").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("assetType")));
	}

	@Test
	void addItemToPortfolio_returns201_withCreatedItem() throws Exception {
		CreatePortfolioItemRequest request = validRequest();
		PortfolioItemDTO created = buildItemDto(11L, 10L, AssetType.STOCK, "AAPL", "Apple");

		when(portfolioItemService.addItemToPortfolio(eq(10L), any(CreatePortfolioItemRequest.class)))
				.thenReturn(created);

		mockMvc.perform(post("/api/portfolios/10/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(11)))
				.andExpect(jsonPath("$.portfolioId", is(10)))
				.andExpect(jsonPath("$.symbol", is("AAPL")));
	}

	@Test
	void addItemToPortfolio_returns400_whenValidationFails() throws Exception {
		CreatePortfolioItemRequest request = new CreatePortfolioItemRequest();
		request.setAssetType(null);
		request.setSymbol("");
		request.setName("");
		request.setQuantity(new BigDecimal("0.0000"));
		request.setPurchasePrice(BigDecimal.ZERO);

		mockMvc.perform(post("/api/portfolios/10/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Validation failed")))
				.andExpect(jsonPath("$.fieldErrors.assetType", is("Asset type is required")))
				.andExpect(jsonPath("$.fieldErrors.symbol", is("Symbol is required")))
				.andExpect(jsonPath("$.fieldErrors.name", is("Name is required")))
				.andExpect(jsonPath("$.fieldErrors.quantity", is("Quantity must be greater than 0")))
				.andExpect(jsonPath("$.fieldErrors.purchasePrice", is("Purchase price must be greater than 0")));
	}

	@Test
	void addItemToPortfolio_returns400_forMalformedJson() throws Exception {
		mockMvc.perform(post("/api/portfolios/10/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"assetType\":\"STOCK\",\"symbol\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void addItemToPortfolio_returns400_forInvalidBodyEnum() throws Exception {
		String requestBody = """
				{
				  \"assetType\": \"NOPE\",
				  \"symbol\": \"AAPL\",
				  \"name\": \"Apple\",
				  \"quantity\": 1.5,
				  \"purchasePrice\": 123.45
				}
				""";

		mockMvc.perform(post("/api/portfolios/10/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void getItemById_returns200_withItem() throws Exception {
		when(portfolioItemService.getItemById(5L))
				.thenReturn(buildItemDto(5L, 10L, AssetType.BOND, "TLT", "Treasury ETF"));

		mockMvc.perform(get("/api/portfolios/10/items/5").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(5)))
				.andExpect(jsonPath("$.assetType", is("BOND")))
				.andExpect(jsonPath("$.name", is("Treasury ETF")));
	}

	@Test
	void updateItem_returns200_withUpdatedItem() throws Exception {
		CreatePortfolioItemRequest request = validRequest();
		request.setSymbol("MSFT");
		request.setName("Microsoft");
		PortfolioItemDTO updated = buildItemDto(7L, 10L, AssetType.STOCK, "MSFT", "Microsoft");

		when(portfolioItemService.updateItem(eq(7L), any(CreatePortfolioItemRequest.class))).thenReturn(updated);

		mockMvc.perform(put("/api/portfolios/10/items/7")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(7)))
				.andExpect(jsonPath("$.symbol", is("MSFT")))
				.andExpect(jsonPath("$.name", is("Microsoft")));
	}

	@Test
	void deleteItem_returns204() throws Exception {
		doNothing().when(portfolioItemService).deleteItem(8L);

		mockMvc.perform(delete("/api/portfolios/10/items/8"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void refreshItemPrice_returns200_withUpdatedPrice() throws Exception {
		PortfolioItemDTO refreshed = buildItemDto(9L, 10L, AssetType.STOCK, "NVDA", "NVIDIA");
		refreshed.setCurrentPrice(new BigDecimal("980.55"));

		when(portfolioItemService.refreshItemPrice(9L)).thenReturn(refreshed);

		mockMvc.perform(post("/api/portfolios/10/items/9/refresh-price").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(9)))
				.andExpect(jsonPath("$.currentPrice", is(980.55)));
	}

	@Test
	void getItemById_returns404_whenServiceThrowsResourceNotFound() throws Exception {
		when(portfolioItemService.getItemById(404L)).thenThrow(new ResourceNotFoundException("Portfolio item not found"));

		mockMvc.perform(get("/api/portfolios/10/items/404").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Portfolio item not found")));
	}

	@Test
	void updateItem_returns400_whenServiceThrowsBadRequest() throws Exception {
		CreatePortfolioItemRequest request = validRequest();
		doThrow(new BadRequestException("Duplicate symbol in portfolio"))
				.when(portfolioItemService).updateItem(eq(15L), any(CreatePortfolioItemRequest.class));

		mockMvc.perform(put("/api/portfolios/10/items/15")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Duplicate symbol in portfolio")));
	}

	private CreatePortfolioItemRequest validRequest() {
		return new CreatePortfolioItemRequest(
				AssetType.STOCK,
				"AAPL",
				"Apple",
				new BigDecimal("2.5000"),
				new BigDecimal("150.00"),
				LocalDateTime.of(2026, 8, 5, 10, 0),
				"core holding"
		);
	}

	private PortfolioItemDTO buildItemDto(Long id, Long portfolioId, AssetType assetType, String symbol, String name) {
		PortfolioItemDTO dto = new PortfolioItemDTO();
		dto.setId(id);
		dto.setPortfolioId(portfolioId);
		dto.setAssetType(assetType);
		dto.setSymbol(symbol);
		dto.setName(name);
		dto.setQuantity(new BigDecimal("2.5000"));
		dto.setPurchasePrice(new BigDecimal("150.00"));
		dto.setCurrentPrice(new BigDecimal("175.25"));
		dto.setPurchaseDate(LocalDateTime.of(2026, 8, 1, 9, 30));
		dto.setNotes("test-note");
		dto.setTotalInvestment(new BigDecimal("375.00"));
		dto.setCurrentValue(new BigDecimal("438.13"));
		dto.setProfitLoss(new BigDecimal("63.13"));
		dto.setProfitLossPercentage(new BigDecimal("16.83"));
		return dto;
	}
}
