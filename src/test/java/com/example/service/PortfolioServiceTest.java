package com.example.service;

import com.example.dto.CreatePortfolioRequest;
import com.example.dto.PortfolioDTO;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.PortfolioSummaryDTO;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.AssetType;
import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.model.RiskLevel;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.PortfolioRepository;
import com.example.repository.UserRepositoryInterface;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioServiceTest {

	@Test
	void getAllPortfolios_returnsMappedPortfolios() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, userRepository);

		Portfolio first = portfolioEntity(1L, 10L, 1L, "Growth", "USD", List.of());
		Portfolio second = portfolioEntity(2L, 10L, 2L, "Income", "EUR", List.of());
		portfolioRepository.findAllResult = List.of(first, second);

		List<PortfolioDTO> result = service.getAllPortfolios();

		assertEquals(2, result.size());
		assertEquals("Growth", result.get(0).getName());
		assertEquals("Income", result.get(1).getName());
		assertEquals(1, portfolioRepository.findAllCalls);
	}

	@Test
	void getPortfoliosByUserId_returnsOnlyUserPortfolios() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, userRepository);

		Long userId = 42L;
		Portfolio portfolio = portfolioEntity(3L, userId, 7L, "Retirement", "USD", List.of());
		portfolioRepository.portfoliosByUserId.put(userId, List.of(portfolio));

		List<PortfolioDTO> result = service.getPortfoliosByUserId(userId);

		assertEquals(1, result.size());
		assertEquals(userId, result.get(0).getUserId());
		assertEquals("Retirement", result.get(0).getName());
		assertEquals(1, portfolioRepository.findByUserIdCalls);
	}

	@Test
	void getPortfolioById_returnsMappedPortfolioWithNestedItems() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, userRepository);

		Portfolio portfolio = new Portfolio();
		portfolio.setId(11L);
		portfolio.setUserId(22L);
		portfolio.setPortfolioNumber(5L);
		portfolio.setName("Tech Portfolio");
		portfolio.setDescription("Long term growth");
		portfolio.setCurrency("USD");
		portfolio.setRiskLevel(RiskLevel.AGGRESSIVE);
		portfolio.setInvestmentGoal(InvestmentGoal.GROWTH);
		portfolio.setTargetValue(new BigDecimal("100000.00"));
		portfolio.setInvestmentHorizon(InvestmentHorizon.LONG_TERM);
		portfolio.setCreatedAt(LocalDateTime.of(2026, 8, 4, 9, 15));
		portfolio.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 9, 30));

		PortfolioItem item = portfolioItemEntity(
				101L,
				portfolio,
				AssetType.STOCK,
				"AAPL",
				"Apple",
				new BigDecimal("5.00"),
				new BigDecimal("10.00"),
				new BigDecimal("12.00"),
				LocalDateTime.of(2026, 8, 1, 14, 0),
				"core holding"
		);
		portfolio.setItems(List.of(item));
		portfolioRepository.portfoliosById.put(11L, portfolio);

		PortfolioDTO result = service.getPortfolioById(11L);

		assertAll(
				() -> assertEquals(11L, result.getId()),
				() -> assertEquals(22L, result.getUserId()),
				() -> assertEquals(5L, result.getPortfolioNumber()),
				() -> assertEquals("Tech Portfolio", result.getName()),
				() -> assertEquals("Long term growth", result.getDescription()),
				() -> assertEquals("USD", result.getCurrency()),
				() -> assertEquals(RiskLevel.AGGRESSIVE, result.getRiskLevel()),
				() -> assertEquals(InvestmentGoal.GROWTH, result.getInvestmentGoal()),
				() -> assertEquals(new BigDecimal("100000.00"), result.getTargetValue()),
				() -> assertEquals(InvestmentHorizon.LONG_TERM, result.getInvestmentHorizon()),
				() -> assertEquals(LocalDateTime.of(2026, 8, 4, 9, 15), result.getCreatedAt()),
				() -> assertEquals(LocalDateTime.of(2026, 8, 4, 9, 30), result.getUpdatedAt())
		);

		assertEquals(1, result.getItems().size());
		PortfolioItemDTO dto = result.getItems().get(0);
		assertAll(
				() -> assertEquals(101L, dto.getId()),
				() -> assertEquals(11L, dto.getPortfolioId()),
				() -> assertEquals(AssetType.STOCK, dto.getAssetType()),
				() -> assertEquals("AAPL", dto.getSymbol()),
				() -> assertEquals("Apple", dto.getName()),
				() -> assertEquals(new BigDecimal("5.00"), dto.getQuantity()),
				() -> assertEquals(new BigDecimal("10.00"), dto.getPurchasePrice()),
				() -> assertEquals(new BigDecimal("12.00"), dto.getCurrentPrice()),
				() -> assertEquals(LocalDateTime.of(2026, 8, 1, 14, 0), dto.getPurchaseDate()),
				() -> assertEquals("core holding", dto.getNotes()),
				() -> assertEquals(new BigDecimal("50.0000"), dto.getTotalInvestment()),
				() -> assertEquals(new BigDecimal("60.0000"), dto.getCurrentValue()),
				() -> assertEquals(new BigDecimal("10.0000"), dto.getProfitLoss()),
				() -> assertEquals(new BigDecimal("20.0000"), dto.getProfitLossPercentage())
		);
		assertEquals(1, portfolioRepository.findByIdCalls);
	}

	@Test
	void getPortfolioById_throwsWhenPortfolioMissing() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.getPortfolioById(99L));

		assertEquals("Portfolio not found with id: 99", exception.getMessage());
		assertEquals(1, portfolioRepository.findByIdCalls);
	}

	@Test
	void createPortfolio_createsPortfolioWithDefaultCurrencyAndNextPortfolioNumber() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), userRepository);

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				7L,
				"My Portfolio",
				"Long-term portfolio",
				null,
				RiskLevel.MODERATE,
				InvestmentGoal.BALANCED,
				new BigDecimal("25000.00"),
				InvestmentHorizon.MEDIUM_TERM
		);
		portfolioRepository.existsByNameResult.put("My Portfolio", false);
		portfolioRepository.nextPortfolioNumberByUserId.put(7L, 3L);
		portfolioRepository.nextSavedId = 100L;
		userRepository.existsByIdResult.put(7L, true);

		PortfolioDTO result = service.createPortfolio(request);

		assertAll(
				() -> assertEquals(100L, result.getId()),
				() -> assertEquals(7L, result.getUserId()),
				() -> assertEquals(3L, result.getPortfolioNumber()),
				() -> assertEquals("My Portfolio", result.getName()),
				() -> assertEquals("Long-term portfolio", result.getDescription()),
				() -> assertEquals("USD", result.getCurrency()),
				() -> assertEquals(RiskLevel.MODERATE, result.getRiskLevel()),
				() -> assertEquals(InvestmentGoal.BALANCED, result.getInvestmentGoal()),
				() -> assertEquals(new BigDecimal("25000.00"), result.getTargetValue()),
				() -> assertEquals(InvestmentHorizon.MEDIUM_TERM, result.getInvestmentHorizon())
		);
		assertEquals(1, portfolioRepository.existsByNameCalls);
		assertEquals(1, userRepository.existsByIdCalls);
		assertEquals(1, portfolioRepository.getNextPortfolioNumberCalls);
		assertEquals(1, portfolioRepository.saveCalls);
		assertEquals("USD", portfolioRepository.lastSaved.getCurrency());
		assertEquals(3L, portfolioRepository.lastSaved.getPortfolioNumber());
	}

	@Test
	void createPortfolio_throwsWhenNameAlreadyExists() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				1L,
				"Duplicate",
				"desc",
				"EUR",
				RiskLevel.CONSERVATIVE,
				InvestmentGoal.INCOME,
				new BigDecimal("1000.00"),
				InvestmentHorizon.SHORT_TERM
		);
		portfolioRepository.existsByNameResult.put("Duplicate", true);

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> service.createPortfolio(request));

		assertEquals("Portfolio with name 'Duplicate' already exists", exception.getMessage());
		assertEquals(1, portfolioRepository.existsByNameCalls);
		assertEquals(0, portfolioRepository.saveCalls);
	}

	@Test
	void createPortfolio_throwsWhenUserIdMissing() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				null,
				"No User",
				"desc",
				"EUR",
				RiskLevel.CONSERVATIVE,
				InvestmentGoal.INCOME,
				new BigDecimal("1000.00"),
				InvestmentHorizon.SHORT_TERM
		);
		portfolioRepository.existsByNameResult.put("No User", false);

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> service.createPortfolio(request));

		assertEquals("User id is required to create a portfolio", exception.getMessage());
		assertEquals(1, portfolioRepository.existsByNameCalls);
		assertEquals(0, portfolioRepository.saveCalls);
	}

	@Test
	void createPortfolio_throwsWhenUserDoesNotExist() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), userRepository);

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				999L,
				"Missing User",
				"desc",
				"EUR",
				RiskLevel.CONSERVATIVE,
				InvestmentGoal.INCOME,
				new BigDecimal("1000.00"),
				InvestmentHorizon.SHORT_TERM
		);
		portfolioRepository.existsByNameResult.put("Missing User", false);
		userRepository.existsByIdResult.put(999L, false);

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.createPortfolio(request));

		assertEquals("User not found with id: 999", exception.getMessage());
		assertEquals(1, portfolioRepository.existsByNameCalls);
		assertEquals(1, userRepository.existsByIdCalls);
		assertEquals(0, portfolioRepository.saveCalls);
	}

	@Test
	void updatePortfolio_updatesPortfolioAndPreservesExistingUserWhenUserIdIsNull() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), userRepository);

		Portfolio existing = portfolioEntity(20L, 8L, 2L, "Old Name", "EUR", List.of());
		existing.setDescription("Old desc");
		existing.setRiskLevel(RiskLevel.CONSERVATIVE);
		existing.setInvestmentGoal(InvestmentGoal.INCOME);
		existing.setTargetValue(new BigDecimal("5000.00"));
		existing.setInvestmentHorizon(InvestmentHorizon.SHORT_TERM);
		portfolioRepository.portfoliosById.put(20L, existing);
		portfolioRepository.saveReturnsInput = true;

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				null,
				"Updated Name",
				"Updated desc",
				"GBP",
				RiskLevel.AGGRESSIVE,
				InvestmentGoal.GROWTH,
				new BigDecimal("8000.00"),
				InvestmentHorizon.LONG_TERM
		);

		PortfolioDTO result = service.updatePortfolio(20L, request);

		assertAll(
				() -> assertEquals(20L, result.getId()),
				() -> assertEquals(8L, result.getUserId()),
				() -> assertEquals("Updated Name", result.getName()),
				() -> assertEquals("Updated desc", result.getDescription()),
				() -> assertEquals("GBP", result.getCurrency()),
				() -> assertEquals(RiskLevel.AGGRESSIVE, result.getRiskLevel()),
				() -> assertEquals(InvestmentGoal.GROWTH, result.getInvestmentGoal()),
				() -> assertEquals(new BigDecimal("8000.00"), result.getTargetValue()),
				() -> assertEquals(InvestmentHorizon.LONG_TERM, result.getInvestmentHorizon())
		);
		assertEquals(1, portfolioRepository.findByIdCalls);
		assertEquals(0, userRepository.existsByIdCalls);
		assertEquals(1, portfolioRepository.saveCalls);
		assertEquals("GBP", portfolioRepository.lastSaved.getCurrency());
	}

	@Test
	void updatePortfolio_throwsWhenPortfolioMissing() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.updatePortfolio(404L, basicRequest(1L)));

		assertEquals("Portfolio not found with id: 404", exception.getMessage());
		assertEquals(1, portfolioRepository.findByIdCalls);
	}

	@Test
	void updatePortfolio_throwsWhenUpdatedUserDoesNotExist() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakeUserRepository userRepository = new FakeUserRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), userRepository);

		Portfolio existing = portfolioEntity(30L, 1L, 9L, "Portfolio", "USD", List.of());
		portfolioRepository.portfoliosById.put(30L, existing);
		userRepository.existsByIdResult.put(77L, false);

		CreatePortfolioRequest request = new CreatePortfolioRequest(
				77L,
				"Portfolio Updated",
				"desc",
				"EUR",
				RiskLevel.MODERATE,
				InvestmentGoal.BALANCED,
				new BigDecimal("9000.00"),
				InvestmentHorizon.MEDIUM_TERM
		);

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.updatePortfolio(30L, request));

		assertEquals("User not found with id: 77", exception.getMessage());
		assertEquals(1, portfolioRepository.findByIdCalls);
		assertEquals(1, userRepository.existsByIdCalls);
		assertEquals(0, portfolioRepository.saveCalls);
	}

	@Test
	void deletePortfolio_deletesExistingPortfolio() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		portfolioRepository.existsByIdResult.put(55L, true);

		service.deletePortfolio(55L);

		assertEquals(1, portfolioRepository.existsByIdCalls);
		assertEquals(55L, portfolioRepository.deletedId);
	}

	@Test
	void deletePortfolio_throwsWhenPortfolioMissing() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		portfolioRepository.existsByIdResult.put(56L, false);

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.deletePortfolio(56L));

		assertEquals("Portfolio not found with id: 56", exception.getMessage());
		assertEquals(1, portfolioRepository.existsByIdCalls);
		assertEquals(null, portfolioRepository.deletedId);
	}

	@Test
	void getPortfolioSummary_throwsWhenPortfolioMissing() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		PortfolioService service = new PortfolioService(portfolioRepository, new FakePortfolioItemRepository(), new FakeMarketDataService(), new FakeUserRepository());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> service.getPortfolioSummary(77L));

		assertEquals("Portfolio not found with id: 77", exception.getMessage());
		assertEquals(1, portfolioRepository.findByIdCalls);
	}

	@Test
	void getPortfolioSummary_returnsZeroTotalsForEmptyPortfolio() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, new FakeUserRepository());

		Portfolio portfolio = portfolioEntity(88L, 1L, 1L, "Empty", "USD", new ArrayList<>());
		portfolioRepository.portfoliosById.put(88L, portfolio);

		PortfolioSummaryDTO summary = service.getPortfolioSummary(88L);

		assertAll(
				() -> assertEquals(88L, summary.getPortfolioId()),
				() -> assertEquals("Empty", summary.getPortfolioName()),
				() -> assertEquals(0, summary.getTotalItems()),
				() -> assertEquals(0, summary.getTotalInvestment().compareTo(BigDecimal.ZERO)),
				() -> assertEquals(0, summary.getCurrentValue().compareTo(BigDecimal.ZERO)),
				() -> assertEquals(0, summary.getTotalProfitLoss().compareTo(BigDecimal.ZERO)),
				() -> assertEquals(0, summary.getTotalProfitLossPercentage().compareTo(BigDecimal.ZERO))
		);
		assertEquals(1, portfolioRepository.findByIdCalls);
		assertTrue(marketDataService.requestedTickers.isEmpty());
	}

	@Test
	void getPortfolioSummary_updatesPricesAndCalculatesTotals() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, new FakeUserRepository());

		Portfolio portfolio = new Portfolio();
		portfolio.setId(90L);
		portfolio.setName("Balanced");

		PortfolioItem stock = portfolioItemEntity(
				201L,
				portfolio,
				AssetType.STOCK,
				"AAPL",
				"Apple",
				new BigDecimal("2.00"),
				new BigDecimal("10.00"),
				new BigDecimal("10.00"),
				LocalDateTime.of(2026, 8, 2, 10, 0),
				null
		);
		PortfolioItem cash = portfolioItemEntity(
				202L,
				portfolio,
				AssetType.CASH,
				"USD",
				"Cash",
				new BigDecimal("1.00"),
				new BigDecimal("5.00"),
				null,
				LocalDateTime.of(2026, 8, 2, 10, 0),
				null
		);
		portfolio.setItems(new ArrayList<>(List.of(stock, cash)));
		portfolioRepository.portfoliosById.put(90L, portfolio);
		marketDataService.prices.put("AAPL", new BigDecimal("15.00"));

		PortfolioSummaryDTO summary = service.getPortfolioSummary(90L);

		assertAll(
				() -> assertEquals(90L, summary.getPortfolioId()),
				() -> assertEquals("Balanced", summary.getPortfolioName()),
				() -> assertEquals(2, summary.getTotalItems()),
				() -> assertEquals(0, summary.getTotalInvestment().compareTo(new BigDecimal("25.00"))),
				() -> assertEquals(0, summary.getCurrentValue().compareTo(new BigDecimal("35.00"))),
				() -> assertEquals(0, summary.getTotalProfitLoss().compareTo(new BigDecimal("10.00"))),
				() -> assertEquals(0, summary.getTotalProfitLossPercentage().compareTo(new BigDecimal("40.0000")))
		);
		assertEquals(new BigDecimal("15.00"), stock.getCurrentPrice());
		assertEquals(null, cash.getCurrentPrice());
		assertEquals(List.of("AAPL", "USD"), marketDataService.requestedTickers);
		assertEquals(1, portfolioRepository.findByIdCalls);
	}

	@Test
	void refreshPortfolioPrices_updatesMarketAssetsAndKeepsNullPricesUnchanged() {
		FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
		FakePortfolioItemRepository portfolioItemRepository = new FakePortfolioItemRepository();
		FakeMarketDataService marketDataService = new FakeMarketDataService();
		PortfolioService service = new PortfolioService(portfolioRepository, portfolioItemRepository, marketDataService, new FakeUserRepository());

		Portfolio portfolio = new Portfolio();
		portfolio.setId(91L);
		portfolio.setName("Refresh Me");

		PortfolioItem etf = portfolioItemEntity(
				301L,
				portfolio,
				AssetType.ETF,
				"QQQ",
				"Nasdaq ETF",
				new BigDecimal("3.00"),
				new BigDecimal("20.00"),
				new BigDecimal("21.00"),
				LocalDateTime.of(2026, 8, 1, 11, 0),
				null
		);
		PortfolioItem other = portfolioItemEntity(
				302L,
				portfolio,
				AssetType.OTHER,
				"DXY",
				"Dollar Index",
				new BigDecimal("1.00"),
				new BigDecimal("1.00"),
				null,
				LocalDateTime.of(2026, 8, 1, 11, 0),
				null
		);
		portfolio.setItems(new ArrayList<>(List.of(etf, other)));
		portfolioRepository.portfoliosById.put(91L, portfolio);
		marketDataService.prices.put("QQQ", null);

		service.refreshPortfolioPrices(91L);

		assertEquals(new BigDecimal("21.00"), etf.getCurrentPrice());
		assertEquals(AssetType.OTHER, other.getAssetType());
		assertEquals(1, portfolioRepository.findByIdCalls);
		assertEquals(List.of("QQQ", "DXY"), marketDataService.requestedTickers);
		assertEquals(1, portfolioItemRepository.saveAllCalls);
		assertEquals(portfolio.getItems(), portfolioItemRepository.lastSavedItems);
	}

	private static Portfolio portfolioEntity(Long id, Long userId, Long portfolioNumber, String name, String currency, List<PortfolioItem> items) {
		Portfolio portfolio = new Portfolio();
		portfolio.setId(id);
		portfolio.setUserId(userId);
		portfolio.setPortfolioNumber(portfolioNumber);
		portfolio.setName(name);
		portfolio.setDescription(name + " description");
		portfolio.setCurrency(currency);
		portfolio.setRiskLevel(RiskLevel.MODERATE);
		portfolio.setInvestmentGoal(InvestmentGoal.BALANCED);
		portfolio.setTargetValue(new BigDecimal("1000.00"));
		portfolio.setInvestmentHorizon(InvestmentHorizon.LONG_TERM);
		portfolio.setCreatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
		portfolio.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 8, 30));
		portfolio.setItems(new ArrayList<>());
		items.forEach(portfolio::addItem);
		return portfolio;
	}

	private PortfolioItem portfolioItemEntity(Long id,
											  Portfolio portfolio,
											  AssetType assetType,
											  String symbol,
											  String name,
											  BigDecimal quantity,
											  BigDecimal purchasePrice,
											  BigDecimal currentPrice,
											  LocalDateTime purchaseDate,
											  String notes) {
		PortfolioItem item = new PortfolioItem();
		item.setId(id);
		item.setPortfolio(portfolio);
		item.setAssetType(assetType);
		item.setSymbol(symbol);
		item.setName(name);
		item.setQuantity(quantity);
		item.setPurchasePrice(purchasePrice);
		item.setCurrentPrice(currentPrice);
		item.setPurchaseDate(purchaseDate);
		item.setCreatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));
		item.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 9, 30));
		item.setNotes(notes);
		return item;
	}

	private CreatePortfolioRequest basicRequest(Long userId) {
		return new CreatePortfolioRequest(
				userId,
				"Portfolio",
				"desc",
				"USD",
				RiskLevel.MODERATE,
				InvestmentGoal.BALANCED,
				new BigDecimal("1000.00"),
				InvestmentHorizon.LONG_TERM
		);
	}

	private static class FakePortfolioRepository extends PortfolioRepository {
		private List<Portfolio> findAllResult = List.of();
		private final Map<Long, List<Portfolio>> portfoliosByUserId = new HashMap<>();
		private final Map<Long, Portfolio> portfoliosById = new HashMap<>();
		private final Map<String, Boolean> existsByNameResult = new HashMap<>();
		private final Map<Long, Boolean> existsByIdResult = new HashMap<>();
		private final Map<Long, Long> nextPortfolioNumberByUserId = new HashMap<>();
		private int findAllCalls;
		private int findByUserIdCalls;
		private int findByIdCalls;
		private int existsByNameCalls;
		private int existsByIdCalls;
		private int getNextPortfolioNumberCalls;
		private int saveCalls;
		private int deleteByIdCalls;
		private Long deletedId;
		private Portfolio lastSaved;
		private Long nextSavedId;
		private boolean saveReturnsInput = true;

		private FakePortfolioRepository() {
			super(null, null);
		}

		@Override
		public List<Portfolio> findAll() {
			findAllCalls++;
			return findAllResult;
		}

		@Override
		public List<Portfolio> findByUserId(Long userId) {
			findByUserIdCalls++;
			return portfoliosByUserId.getOrDefault(userId, List.of());
		}

		@Override
		public Optional<Portfolio> findById(Long id) {
			findByIdCalls++;
			return Optional.ofNullable(portfoliosById.get(id));
		}

		@Override
		public boolean existsById(Long id) {
			existsByIdCalls++;
			return existsByIdResult.getOrDefault(id, false);
		}

		@Override
		public boolean existsByName(String name) {
			existsByNameCalls++;
			return existsByNameResult.getOrDefault(name, false);
		}

		@Override
		public Long getNextPortfolioNumberByUserId(Long userId) {
			getNextPortfolioNumberCalls++;
			return nextPortfolioNumberByUserId.getOrDefault(userId, 1L);
		}

		@Override
		public Portfolio save(Portfolio portfolio) {
			saveCalls++;
			lastSaved = portfolio;
			if (portfolio.getId() == null && nextSavedId != null) {
				portfolio.setId(nextSavedId);
			}
			if (saveReturnsInput) {
				if (portfolio.getCreatedAt() == null) {
					portfolio.setCreatedAt(LocalDateTime.of(2026, 8, 4, 10, 0));
				}
				if (portfolio.getUpdatedAt() == null) {
					portfolio.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 10, 0));
				}
				portfoliosById.put(portfolio.getId(), portfolio);
				return portfolio;
			}
			Portfolio copy = portfolioEntity(portfolio.getId(), portfolio.getUserId(), portfolio.getPortfolioNumber(), portfolio.getName(), portfolio.getCurrency(), List.of());
			copy.setDescription(portfolio.getDescription());
			copy.setRiskLevel(portfolio.getRiskLevel());
			copy.setInvestmentGoal(portfolio.getInvestmentGoal());
			copy.setTargetValue(portfolio.getTargetValue());
			copy.setInvestmentHorizon(portfolio.getInvestmentHorizon());
			copy.setCreatedAt(portfolio.getCreatedAt());
			copy.setUpdatedAt(portfolio.getUpdatedAt());
			copy.setItems(portfolio.getItems());
			portfoliosById.put(copy.getId(), copy);
			return copy;
		}

		@Override
		public void deleteById(Long id) {
			deleteByIdCalls++;
			deletedId = id;
		}
	}

	private static class FakePortfolioItemRepository extends PortfolioItemRepository {
		private int saveAllCalls;
		private List<PortfolioItem> lastSavedItems = List.of();

		private FakePortfolioItemRepository() {
			super(null);
		}

		@Override
		public List<PortfolioItem> saveAll(List<PortfolioItem> items) {
			saveAllCalls++;
			lastSavedItems = items;
			return items;
		}
	}

	private static class FakeMarketDataService extends MarketDataService {
		private final Map<String, BigDecimal> prices = new HashMap<>();
		private final List<String> requestedTickers = new ArrayList<>();

		FakeMarketDataService() {
			super(null);
		}

		@Override
		public BigDecimal getCurrentPrice(String ticker) {
			requestedTickers.add(ticker);
			return prices.get(ticker);
		}
	}

	private static class FakeUserRepository implements UserRepositoryInterface {
		private final Map<Long, Boolean> existsByIdResult = new HashMap<>();
		private int existsByIdCalls;

		@Override
		public List<com.example.model.User> findAll() {
			return List.of();
		}

		@Override
		public Optional<com.example.model.User> findById(Long userId) {
			return Optional.empty();
		}

		@Override
		public Optional<com.example.model.User> findByUsername(String username) {
			return Optional.empty();
		}

		@Override
		public boolean existsById(Long userId) {
			existsByIdCalls++;
			return existsByIdResult.getOrDefault(userId, false);
		}

		@Override
		public boolean existsByUsername(String username) {
			return false;
		}

		@Override
		public com.example.model.User save(com.example.model.User user) {
			return user;
		}

		@Override
		public Optional<BigDecimal> getWalletBalance(Long userId) {
			return Optional.empty();
		}

		@Override
		public boolean addMoney(Long userId, BigDecimal amount) {
			return false;
		}

		@Override
		public boolean removeMoney(Long userId, BigDecimal amount) {
			return false;
		}

		@Override
		public void deleteById(Long userId) {
		}
	}
}
