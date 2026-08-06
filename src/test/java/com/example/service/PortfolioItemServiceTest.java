package com.example.service;

import com.example.dto.BuyAssetRequest;
import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.SellAssetRequest;
import com.example.dto.WalletTransactionDTO;
import com.example.exception.BadRequestException;
import com.example.exception.InsufficientPortfolioQuantityException;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.AssetType;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioItemRepository;
import com.example.repository.PortfolioRepository;
import com.example.repository.UserRepositoryInterface;
import com.example.repository.WalletTransactionRepositoryInterface;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioItemServiceTest {

    @Test
    void getPortfolioItems_returnsMappedItems() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(10L, 7L, "Core");
        ctx.itemRepository.save(item(1L, portfolio, AssetType.STOCK, "AAPL", "Apple", "2.00", "100.00", "120.00"));
        ctx.itemRepository.save(item(2L, portfolio, AssetType.ETF, "QQQ", "Invesco QQQ", "1.50", "300.00", "320.00"));

        List<PortfolioItemDTO> result = ctx.service.getPortfolioItems(10L);

        assertEquals(2, result.size());
        assertEquals(List.of("AAPL", "QQQ"), result.stream().map(PortfolioItemDTO::getSymbol).sorted().toList());
        assertEquals(1, ctx.itemRepository.findByPortfolioIdCalls);
    }

    @Test
    void getItemsByAssetType_returnsOnlyMatchingItems() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(10L, 7L, "Core");
        ctx.itemRepository.save(item(1L, portfolio, AssetType.STOCK, "AAPL", "Apple", "2.00", "100.00", "120.00"));
        ctx.itemRepository.save(item(2L, portfolio, AssetType.ETF, "QQQ", "Invesco QQQ", "1.50", "300.00", "320.00"));

        List<PortfolioItemDTO> result = ctx.service.getItemsByAssetType(10L, AssetType.ETF);

        assertEquals(1, result.size());
        assertEquals("QQQ", result.get(0).getSymbol());
        assertEquals(1, ctx.itemRepository.findByPortfolioIdAndAssetTypeCalls);
    }

    @Test
    void getItemById_returnsMappedItem() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(11L, 8L, "Growth");
        PortfolioItem stored = item(15L, portfolio, AssetType.STOCK, "MSFT", "Microsoft", "3.00", "200.00", "220.00");
        ctx.itemRepository.save(stored);

        PortfolioItemDTO result = ctx.service.getItemById(15L);

        assertAll(
                () -> assertEquals(15L, result.getId()),
                () -> assertEquals(11L, result.getPortfolioId()),
                () -> assertEquals(AssetType.STOCK, result.getAssetType()),
                () -> assertEquals("MSFT", result.getSymbol()),
                () -> assertEquals(new BigDecimal("3.00"), result.getQuantity()),
                () -> assertEquals(new BigDecimal("600.0000"), result.getTotalInvestment()),
                () -> assertEquals(new BigDecimal("660.0000"), result.getCurrentValue()),
                () -> assertEquals(new BigDecimal("60.0000"), result.getProfitLoss())
        );
        assertEquals(1, ctx.itemRepository.findByIdCalls);
    }

    @Test
    void getItemById_throwsWhenMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.getItemById(999L));

        assertEquals("Portfolio item not found with id: 999", ex.getMessage());
    }

    @Test
    void addItemToPortfolio_setsLivePriceForStock() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(20L, 9L, "Tech");
        ctx.portfolioRepository.portfoliosById.put(20L, portfolio);
        ctx.marketDataService.prices.put("aapl", new BigDecimal("175.25"));

        PortfolioItemDTO result = ctx.service.addItemToPortfolio(20L,
                createItemRequest(AssetType.STOCK, "aapl", "Apple", "4.00", "150.00"));

        assertAll(
                () -> assertEquals("AAPL", result.getSymbol()),
                () -> assertEquals(new BigDecimal("175.25"), result.getCurrentPrice()),
                () -> assertEquals(1, ctx.itemRepository.saveCalls),
                () -> assertEquals(List.of("aapl"), ctx.marketDataService.requestedTickers)
        );
    }

    @Test
    void addItemToPortfolio_usesPurchasePriceWhenLivePriceMissingForEtf() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(21L, 9L, "Index");
        ctx.portfolioRepository.portfoliosById.put(21L, portfolio);
        ctx.marketDataService.prices.put("qqq", null);

        PortfolioItemDTO result = ctx.service.addItemToPortfolio(21L,
                createItemRequest(AssetType.ETF, "qqq", "QQQ", "2.50", "480.00"));

        assertEquals(new BigDecimal("480.00"), result.getCurrentPrice());
        assertEquals(List.of("qqq"), ctx.marketDataService.requestedTickers);
    }

    @Test
    void addItemToPortfolio_usesPurchasePriceForNonMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(22L, 9L, "Cash");
        ctx.portfolioRepository.portfoliosById.put(22L, portfolio);

        PortfolioItemDTO result = ctx.service.addItemToPortfolio(22L,
                createItemRequest(AssetType.CASH, "usd", "US Dollar", "50.00", "1.00"));

        assertEquals("USD", result.getSymbol());
        assertEquals(new BigDecimal("1.00"), result.getCurrentPrice());
        assertEquals(List.of("usd"), ctx.marketDataService.requestedTickers);
    }

    @Test
    void addItemToPortfolio_throwsWhenPortfolioMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.addItemToPortfolio(404L, createItemRequest(AssetType.STOCK, "AAPL", "Apple", "1.00", "100.00")));

        assertEquals("Portfolio not found with id: 404", ex.getMessage());
    }

    @Test
    void buyAsset_createsNewItemAndDebitsWallet() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(30L, 100L, "Growth");
        ctx.portfolioRepository.portfoliosById.put(30L, portfolio);
        ctx.userRepository.walletBalances.put(100L, new BigDecimal("1000.00"));
        ctx.marketDataService.prices.put("tsla", new BigDecimal("250.12"));

        PortfolioItemDTO result = ctx.service.buyAsset(100L, 30L,
                new BuyAssetRequest(AssetType.STOCK, "tsla", "Tesla", new BigDecimal("2.00")));

        assertAll(
                () -> assertEquals("TSLA", result.getSymbol()),
                () -> assertEquals(new BigDecimal("2.00"), result.getQuantity()),
                () -> assertEquals(new BigDecimal("250.12"), result.getPurchasePrice()),
                () -> assertEquals(new BigDecimal("250.12"), result.getCurrentPrice()),
                () -> assertEquals("Created from BUY transaction", result.getNotes()),
                () -> assertNotNull(result.getPurchaseDate()),
                () -> assertEquals(new BigDecimal("499.76"), ctx.userRepository.walletBalances.get(100L)),
                () -> assertEquals(1, ctx.walletTransactionRepository.savedTransactions.size())
        );
        SavedWalletTransaction tx = ctx.walletTransactionRepository.savedTransactions.get(0);
        assertAll(
                () -> assertEquals("BUY", tx.transactionType),
                () -> assertEquals(new BigDecimal("500.24"), tx.amount),
                () -> assertEquals(new BigDecimal("1000.00"), tx.before),
                () -> assertEquals(new BigDecimal("499.76"), tx.after)
        );
    }

    @Test
    void buyAsset_mergesExistingItemUsingWeightedAveragePurchasePrice() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(31L, 101L, "Merge");
        ctx.portfolioRepository.portfoliosById.put(31L, portfolio);
        ctx.userRepository.walletBalances.put(101L, new BigDecimal("2000.00"));
        ctx.marketDataService.prices.put("nvda", new BigDecimal("130.00"));
        ctx.itemRepository.save(item(51L, portfolio, AssetType.STOCK, "NVDA", "NVIDIA", "2.00", "100.00", "110.00"));

        PortfolioItemDTO result = ctx.service.buyAsset(101L, 31L,
                new BuyAssetRequest(AssetType.STOCK, "nvda", "NVIDIA Updated", new BigDecimal("3.00")));

        assertAll(
                () -> assertEquals(new BigDecimal("5.00"), result.getQuantity()),
                () -> assertEquals(new BigDecimal("118.00"), result.getPurchasePrice()),
                () -> assertEquals(new BigDecimal("130.00"), result.getCurrentPrice()),
                () -> assertEquals("NVIDIA Updated", result.getName())
        );
    }

    @Test
    void buyAsset_supportsEtfWithLiveMarketPrice() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(31_1L, 101_1L, "ETF Buy");
        ctx.portfolioRepository.portfoliosById.put(31_1L, portfolio);
        ctx.userRepository.walletBalances.put(101_1L, new BigDecimal("1000.00"));
        ctx.marketDataService.prices.put("spy", new BigDecimal("510.00"));

        PortfolioItemDTO result = ctx.service.buyAsset(101_1L, 31_1L,
                new BuyAssetRequest(AssetType.ETF, "spy", "SPY ETF", new BigDecimal("1.00")));

        assertEquals(AssetType.ETF, result.getAssetType());
        assertEquals(new BigDecimal("490.00"), ctx.userRepository.walletBalances.get(101_1L));
    }

    @Test
    void buyAsset_throwsWhenPortfolioBelongsToDifferentUser() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(32L, portfolio(32L, 200L, "Private"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.buyAsset(201L, 32L, new BuyAssetRequest(AssetType.STOCK, "AAPL", "Apple", new BigDecimal("1.00"))));

        assertEquals("Portfolio not found with id: 32 for user id: 201", ex.getMessage());
    }

    @Test
    void buyAsset_throwsWhenAssetTypeIsUnsupported() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(33L, portfolio(33L, 202L, "Alt"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ctx.service.buyAsset(202L, 33L, new BuyAssetRequest(AssetType.CRYPTO, "BTC-USD", "Bitcoin", new BigDecimal("0.50"))));

        assertEquals("Unable to fetch live market price for symbol: BTC-USD", ex.getMessage());
    }

    @Test
    void buyAsset_throwsWhenLiveMarketPriceUnavailable() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(34L, portfolio(34L, 203L, "Tech"));
        ctx.marketDataService.prices.put("aapl", null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ctx.service.buyAsset(203L, 34L, new BuyAssetRequest(AssetType.STOCK, "aapl", "Apple", new BigDecimal("1.00"))));

        assertEquals("Unable to fetch live market price for symbol: aapl", ex.getMessage());
    }

    @Test
    void buyAsset_throwsWhenWalletUserMissing() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(35L, portfolio(35L, 204L, "Tech"));
        ctx.marketDataService.prices.put("msft", new BigDecimal("400.00"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.buyAsset(204L, 35L, new BuyAssetRequest(AssetType.STOCK, "msft", "Microsoft", new BigDecimal("1.00"))));

        assertEquals("User not found with id: 204", ex.getMessage());
    }

    @Test
    void buyAsset_throwsWhenWalletBalanceInsufficient() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(36L, portfolio(36L, 205L, "Tech"));
        ctx.marketDataService.prices.put("aapl", new BigDecimal("200.00"));
        ctx.userRepository.walletBalances.put(205L, new BigDecimal("150.00"));

        InsufficientWalletBalanceException ex = assertThrows(InsufficientWalletBalanceException.class,
                () -> ctx.service.buyAsset(205L, 36L, new BuyAssetRequest(AssetType.STOCK, "aapl", "Apple", new BigDecimal("1.00"))));

        assertEquals("Insufficient wallet balance for user id 205 to withdraw amount 200.00", ex.getMessage());
        assertEquals(0, ctx.userRepository.removeMoneyCalls);
    }

    @Test
    void buyAsset_throwsWhenWalletDebitFails() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(37L, portfolio(37L, 206L, "Tech"));
        ctx.marketDataService.prices.put("aapl", new BigDecimal("100.00"));
        ctx.userRepository.walletBalances.put(206L, new BigDecimal("500.00"));
        ctx.userRepository.forceRemoveMoneyResult = false;

        InsufficientWalletBalanceException ex = assertThrows(InsufficientWalletBalanceException.class,
                () -> ctx.service.buyAsset(206L, 37L, new BuyAssetRequest(AssetType.STOCK, "aapl", "Apple", new BigDecimal("2.00"))));

        assertEquals("Insufficient wallet balance for user id 206 to withdraw amount 200.00", ex.getMessage());
        assertTrue(ctx.walletTransactionRepository.savedTransactions.isEmpty());
    }

    @Test
    void buyAsset_throwsWhenExistingSymbolHasDifferentAssetType() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(38L, 207L, "Tech");
        ctx.portfolioRepository.portfoliosById.put(38L, portfolio);
        ctx.userRepository.walletBalances.put(207L, new BigDecimal("1000.00"));
        ctx.marketDataService.prices.put("spy", new BigDecimal("500.00"));
        ctx.itemRepository.save(item(88L, portfolio, AssetType.ETF, "SPY", "SPY ETF", "1.00", "450.00", "460.00"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ctx.service.buyAsset(207L, 38L, new BuyAssetRequest(AssetType.STOCK, "spy", "SPY Stock?", new BigDecimal("1.00"))));

        assertEquals("Symbol SPY already exists with a different asset type", ex.getMessage());
    }

    @Test
    void sellAsset_sellsPartialQuantityUsingLiveMarketPrice() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(40L, 300L, "Income");
        ctx.portfolioRepository.portfoliosById.put(40L, portfolio);
        ctx.userRepository.walletBalances.put(300L, new BigDecimal("100.00"));
        ctx.marketDataService.prices.put("ko", new BigDecimal("60.00"));
        ctx.itemRepository.save(item(91L, portfolio, AssetType.STOCK, "KO", "Coca-Cola", "5.00", "50.00", "55.00"));

        PortfolioItemDTO result = ctx.service.sellAsset(300L, 40L,
                new SellAssetRequest("ko", new BigDecimal("2.00"), null));

        assertAll(
                () -> assertEquals(new BigDecimal("3.00"), result.getQuantity()),
                () -> assertEquals(new BigDecimal("60.00"), result.getCurrentPrice()),
                () -> assertEquals(new BigDecimal("220.00"), ctx.userRepository.walletBalances.get(300L)),
                () -> assertEquals(1, ctx.walletTransactionRepository.savedTransactions.size())
        );
        SavedWalletTransaction tx = ctx.walletTransactionRepository.savedTransactions.get(0);
        assertEquals("SELL", tx.transactionType);
        assertEquals(new BigDecimal("120.00"), tx.amount);
    }

    @Test
    void sellAsset_usesRequestedOverrideWhenLivePriceMissingForStock() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(41L, 301L, "Override" );
        ctx.portfolioRepository.portfoliosById.put(41L, portfolio);
        ctx.userRepository.walletBalances.put(301L, new BigDecimal("10.00"));
        ctx.marketDataService.prices.put("meta", null);
        ctx.itemRepository.save(item(92L, portfolio, AssetType.STOCK, "META", "Meta", "4.00", "100.00", null));

        PortfolioItemDTO result = ctx.service.sellAsset(301L, 41L,
                new SellAssetRequest("meta", new BigDecimal("1.50"), new BigDecimal("500.00")));

        assertEquals(new BigDecimal("2.50"), result.getQuantity());
        assertEquals(new BigDecimal("500.00"), result.getCurrentPrice());
        assertEquals(new BigDecimal("760.00"), ctx.userRepository.walletBalances.get(301L));
    }

    @Test
    void sellAsset_usesStoredCurrentPriceWhenLivePriceUnavailableAndNoOverride() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(42L, 302L, "Stored Current");
        ctx.portfolioRepository.portfoliosById.put(42L, portfolio);
        ctx.userRepository.walletBalances.put(302L, new BigDecimal("50.00"));
        ctx.marketDataService.prices.put("amzn", null);
        ctx.itemRepository.save(item(93L, portfolio, AssetType.STOCK, "AMZN", "Amazon", "4.00", "100.00", "125.00"));

        PortfolioItemDTO result = ctx.service.sellAsset(302L, 42L,
                new SellAssetRequest("amzn", new BigDecimal("1.00"), null));

        assertEquals(new BigDecimal("3.00"), result.getQuantity());
        assertEquals(new BigDecimal("125.00"), result.getCurrentPrice());
        assertEquals(new BigDecimal("175.00"), ctx.userRepository.walletBalances.get(302L));
    }

    @Test
    void sellAsset_deletesItemWhenAllQuantitySoldUsingStoredPurchasePriceFallback() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(43L, 303L, "Full Sell");
        ctx.portfolioRepository.portfoliosById.put(43L, portfolio);
        ctx.userRepository.walletBalances.put(303L, new BigDecimal("50.00"));
        ctx.marketDataService.prices.put("nflx", null);
        ctx.itemRepository.save(item(94L, portfolio, AssetType.STOCK, "NFLX", "Netflix", "2.00", "80.00", null));

        PortfolioItemDTO result = ctx.service.sellAsset(303L, 43L,
                new SellAssetRequest("nflx", new BigDecimal("2.00"), null));

        assertEquals(new BigDecimal("0"), result.getQuantity());
        assertEquals(new BigDecimal("80.00"), result.getCurrentPrice());
        assertEquals(94L, ctx.itemRepository.deletedIds.get(0));
        assertEquals(new BigDecimal("210.00"), ctx.userRepository.walletBalances.get(303L));
    }

    @Test
    void sellAsset_usesRequestedOverrideForNonMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(44L, 304L, "Cashout");
        ctx.portfolioRepository.portfoliosById.put(44L, portfolio);
        ctx.userRepository.walletBalances.put(304L, new BigDecimal("5.00"));
        ctx.itemRepository.save(item(95L, portfolio, AssetType.CASH, "USD", "Cash", "10.00", "1.00", "1.00"));

        PortfolioItemDTO result = ctx.service.sellAsset(304L, 44L,
                new SellAssetRequest("usd", new BigDecimal("2.50"), new BigDecimal("1.20")));

        assertEquals(new BigDecimal("7.50"), result.getQuantity());
        assertEquals(new BigDecimal("1.20"), result.getCurrentPrice());
        assertEquals(new BigDecimal("8.00"), ctx.userRepository.walletBalances.get(304L));
    }

    @Test
    void sellAsset_throwsWhenPortfolioItemMissing() {
        TestContext ctx = new TestContext();
        ctx.portfolioRepository.portfoliosById.put(45L, portfolio(45L, 305L, "Empty"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.sellAsset(305L, 45L, new SellAssetRequest("IBM", new BigDecimal("1.00"), null)));

        assertEquals("Portfolio item not found for symbol: IBM", ex.getMessage());
    }

    @Test
    void sellAsset_throwsWhenQuantityInsufficient() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(46L, 306L, "Small" );
        ctx.portfolioRepository.portfoliosById.put(46L, portfolio);
        ctx.itemRepository.save(item(96L, portfolio, AssetType.STOCK, "IBM", "IBM", "1.00", "140.00", "150.00"));

        InsufficientPortfolioQuantityException ex = assertThrows(InsufficientPortfolioQuantityException.class,
                () -> ctx.service.sellAsset(306L, 46L, new SellAssetRequest("ibm", new BigDecimal("2.00"), null)));

        assertEquals("Insufficient quantity for symbol IBM in portfolio id 46. Requested: 2.00, available: 1.00", ex.getMessage());
    }

    @Test
    void sellAsset_throwsWhenWalletUserMissing() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(47L, 307L, "No Wallet");
        ctx.portfolioRepository.portfoliosById.put(47L, portfolio);
        ctx.marketDataService.prices.put("orcl", new BigDecimal("140.00"));
        ctx.itemRepository.save(item(97L, portfolio, AssetType.STOCK, "ORCL", "Oracle", "1.00", "100.00", "110.00"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.sellAsset(307L, 47L, new SellAssetRequest("orcl", new BigDecimal("1.00"), null)));

        assertEquals("User not found with id: 307", ex.getMessage());
    }

    @Test
    void sellAsset_throwsWhenWalletCreditFails() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(48L, 308L, "Credit Fail");
        ctx.portfolioRepository.portfoliosById.put(48L, portfolio);
        ctx.userRepository.walletBalances.put(308L, new BigDecimal("20.00"));
        ctx.userRepository.forceAddMoneyResult = false;
        ctx.marketDataService.prices.put("pep", new BigDecimal("180.00"));
        ctx.itemRepository.save(item(98L, portfolio, AssetType.STOCK, "PEP", "Pepsi", "1.00", "150.00", "160.00"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ctx.service.sellAsset(308L, 48L, new SellAssetRequest("pep", new BigDecimal("1.00"), null)));

        assertEquals("Failed to credit wallet for user id 308", ex.getMessage());
        assertTrue(ctx.walletTransactionRepository.savedTransactions.isEmpty());
    }

    @Test
    void sellAsset_throwsWhenNoExecutionPriceAvailableForMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(49L, 309L, "No Price");
        ctx.portfolioRepository.portfoliosById.put(49L, portfolio);
        ctx.userRepository.walletBalances.put(309L, new BigDecimal("20.00"));
        ctx.marketDataService.prices.put("crm", null);
        ctx.itemRepository.save(item(99L, portfolio, AssetType.STOCK, "CRM", "Salesforce", "1.00", null, null));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ctx.service.sellAsset(309L, 49L, new SellAssetRequest("crm", new BigDecimal("1.00"), null)));

        assertEquals("Unable to resolve execution price for symbol: CRM", ex.getMessage());
    }

    @Test
    void sellAsset_throwsWhenNoExecutionPriceAvailableForNonMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(50L, 310L, "No NonMarket Price");
        ctx.portfolioRepository.portfoliosById.put(50L, portfolio);
        ctx.userRepository.walletBalances.put(310L, new BigDecimal("20.00"));
        ctx.itemRepository.save(item(100L, portfolio, AssetType.CASH, "USD", "Cash", "10.00", null, null));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ctx.service.sellAsset(310L, 50L, new SellAssetRequest("usd", new BigDecimal("1.00"), null)));

        assertEquals("Unable to resolve execution price for symbol: USD", ex.getMessage());
    }

    @Test
    void updateItem_refreshesMarketPriceForStock() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(60L, 400L, "Update");
        ctx.marketDataService.prices.put("amd", new BigDecimal("170.00"));
        ctx.itemRepository.save(item(111L, portfolio, AssetType.STOCK, "AMD", "AMD", "2.00", "150.00", "155.00"));

        PortfolioItemDTO result = ctx.service.updateItem(111L,
                createItemRequest(AssetType.STOCK, "amd", "AMD Updated", "3.00", "140.00"));

        assertAll(
                () -> assertEquals("AMD", result.getSymbol()),
                () -> assertEquals("AMD Updated", result.getName()),
                () -> assertEquals(new BigDecimal("3.00"), result.getQuantity()),
                () -> assertEquals(new BigDecimal("170.00"), result.getCurrentPrice())
        );
    }

    @Test
    void updateItem_preservesCurrentPriceWhenLivePriceUnavailable() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(61L, 401L, "Update");
        ctx.marketDataService.prices.put("intc", null);
        ctx.itemRepository.save(item(112L, portfolio, AssetType.STOCK, "INTC", "Intel", "2.00", "30.00", "33.00"));

        PortfolioItemDTO result = ctx.service.updateItem(112L,
                createItemRequest(AssetType.STOCK, "intc", "Intel Updated", "2.50", "31.00"));

        assertEquals(new BigDecimal("33.00"), result.getCurrentPrice());
        assertEquals("INTC", result.getSymbol());
    }

    @Test
    void updateItem_updatesNonMarketAssetWithoutCallingMarketData() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(61_1L, 401_1L, "Update Non Market");
        ctx.itemRepository.save(item(112_1L, portfolio, AssetType.CASH, "USD", "Cash", "5.00", "1.00", "1.00"));

        PortfolioItemDTO result = ctx.service.updateItem(112_1L,
                createItemRequest(AssetType.CASH, "usd", "Cash Updated", "7.50", "1.00"));

        assertAll(
                () -> assertEquals(AssetType.CASH, result.getAssetType()),
                () -> assertEquals("USD", result.getSymbol()),
                () -> assertEquals(new BigDecimal("1.00"), result.getCurrentPrice()),
                () -> assertEquals(List.of("usd"), ctx.marketDataService.requestedTickers)
        );
    }

    @Test
    void updateItem_throwsWhenMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.updateItem(999L, createItemRequest(AssetType.STOCK, "AAPL", "Apple", "1.00", "100.00")));

        assertEquals("Portfolio item not found with id: 999", ex.getMessage());
    }

    @Test
    void deleteItem_deletesExistingItem() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(62L, 402L, "Delete");
        ctx.itemRepository.save(item(113L, portfolio, AssetType.CASH, "USD", "Cash", "5.00", "1.00", "1.00"));

        ctx.service.deleteItem(113L);

        assertEquals(List.of(113L), ctx.itemRepository.deletedIds);
    }

    @Test
    void deleteItem_throwsWhenMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.deleteItem(114L));

        assertEquals("Portfolio item not found with id: 114", ex.getMessage());
    }

    @Test
    void refreshItemPrice_updatesAndSavesMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(63L, 403L, "Refresh");
        ctx.marketDataService.prices.put("adbe", new BigDecimal("610.00"));
        ctx.itemRepository.save(item(115L, portfolio, AssetType.STOCK, "ADBE", "Adobe", "1.00", "500.00", "520.00"));

        PortfolioItemDTO result = ctx.service.refreshItemPrice(115L);

        assertEquals(new BigDecimal("610.00"), result.getCurrentPrice());
        assertEquals(2, ctx.itemRepository.saveCalls);
    }

    @Test
    void refreshItemPrice_returnsUnchangedWhenLivePriceUnavailable() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(64L, 404L, "Refresh");
        ctx.marketDataService.prices.put("shop", null);
        ctx.itemRepository.save(item(116L, portfolio, AssetType.STOCK, "SHOP", "Shopify", "1.00", "70.00", "71.00"));
        int saveCallsBefore = ctx.itemRepository.saveCalls;

        PortfolioItemDTO result = ctx.service.refreshItemPrice(116L);

        assertEquals(new BigDecimal("71.00"), result.getCurrentPrice());
        assertEquals(saveCallsBefore, ctx.itemRepository.saveCalls);
    }

    @Test
    void refreshItemPrice_returnsUnchangedForNonMarketAsset() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(65L, 405L, "Refresh");
        ctx.itemRepository.save(item(117L, portfolio, AssetType.CASH, "USD", "Cash", "20.00", "1.00", "1.00"));
        int saveCallsBefore = ctx.itemRepository.saveCalls;

        PortfolioItemDTO result = ctx.service.refreshItemPrice(117L);

        assertEquals(new BigDecimal("1.00"), result.getCurrentPrice());
        assertEquals(saveCallsBefore, ctx.itemRepository.saveCalls);
        assertEquals(List.of("USD"), ctx.marketDataService.requestedTickers);
    }

    @Test
    void refreshItemPrice_throwsWhenMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ctx.service.refreshItemPrice(118L));

        assertEquals("Portfolio item not found with id: 118", ex.getMessage());
    }

    private static CreatePortfolioItemRequest createItemRequest(AssetType assetType,
                                                                String symbol,
                                                                String name,
                                                                String quantity,
                                                                String purchasePrice) {
        return new CreatePortfolioItemRequest(
                assetType,
                symbol,
                name,
                new BigDecimal(quantity),
                new BigDecimal(purchasePrice),
                LocalDateTime.of(2026, 8, 4, 9, 0),
                "test notes"
        );
    }

    private static Portfolio portfolio(Long id, Long userId, String name) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(id);
        portfolio.setUserId(userId);
        portfolio.setPortfolioNumber(1L);
        portfolio.setName(name);
        portfolio.setItems(new ArrayList<>());
        return portfolio;
    }

    private static PortfolioItem item(Long id,
                                      Portfolio portfolio,
                                      AssetType assetType,
                                      String symbol,
                                      String name,
                                      String quantity,
                                      String purchasePrice,
                                      String currentPrice) {
        PortfolioItem item = new PortfolioItem();
        item.setId(id);
        item.setPortfolio(portfolio);
        item.setAssetType(assetType);
        item.setSymbol(symbol);
        item.setName(name);
        item.setQuantity(new BigDecimal(quantity));
        item.setPurchasePrice(purchasePrice == null ? null : new BigDecimal(purchasePrice));
        item.setCurrentPrice(currentPrice == null ? null : new BigDecimal(currentPrice));
        item.setPurchaseDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        item.setNotes("fixture");
        return item;
    }

    private static final class TestContext {
        private final FakePortfolioItemRepository itemRepository = new FakePortfolioItemRepository();
        private final FakePortfolioRepository portfolioRepository = new FakePortfolioRepository();
        private final FakeMarketDataService marketDataService = new FakeMarketDataService();
        private final FakeUserRepository userRepository = new FakeUserRepository();
        private final FakeWalletTransactionRepository walletTransactionRepository = new FakeWalletTransactionRepository();
        private final PortfolioItemService service = new PortfolioItemService(
                itemRepository,
                portfolioRepository,
                marketDataService,
                userRepository,
                walletTransactionRepository
        );
    }

    private static final class FakePortfolioItemRepository extends PortfolioItemRepository {
        private final Map<Long, PortfolioItem> itemsById = new HashMap<>();
        private int saveCalls;
        private int findByPortfolioIdCalls;
        private int findByPortfolioIdAndAssetTypeCalls;
        private int findByIdCalls;
        private final List<Long> deletedIds = new ArrayList<>();
        private long nextId = 1000L;

        private FakePortfolioItemRepository() {
            super(null);
        }

        @Override
        public List<PortfolioItem> findByPortfolioId(Long portfolioId) {
            findByPortfolioIdCalls++;
            return itemsById.values().stream()
                    .filter(item -> item.getPortfolio().getId().equals(portfolioId))
                    .sorted(Comparator.comparing(PortfolioItem::getId))
                    .collect(Collectors.toList());
        }

        @Override
        public List<PortfolioItem> findByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType) {
            findByPortfolioIdAndAssetTypeCalls++;
            return itemsById.values().stream()
                    .filter(item -> item.getPortfolio().getId().equals(portfolioId))
                    .filter(item -> item.getAssetType() == assetType)
                    .sorted(Comparator.comparing(PortfolioItem::getId))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<PortfolioItem> findByPortfolioIdAndSymbol(Long portfolioId, String symbol) {
            return itemsById.values().stream()
                    .filter(item -> item.getPortfolio().getId().equals(portfolioId))
                    .filter(item -> item.getSymbol().equals(symbol))
                    .findFirst();
        }

        @Override
        public Optional<PortfolioItem> findById(Long id) {
            findByIdCalls++;
            return Optional.ofNullable(itemsById.get(id));
        }

        @Override
        public boolean existsById(Long id) {
            return itemsById.containsKey(id);
        }

        @Override
        public PortfolioItem save(PortfolioItem item) {
            saveCalls++;
            if (item.getId() == null) {
                item.setId(nextId++);
            }
            itemsById.put(item.getId(), item);
            return item;
        }

        @Override
        public void deleteById(Long id) {
            deletedIds.add(id);
            itemsById.remove(id);
        }
    }

    private static final class FakePortfolioRepository extends PortfolioRepository {
        private final Map<Long, Portfolio> portfoliosById = new HashMap<>();

        private FakePortfolioRepository() {
            super(null, null);
        }

        @Override
        public Optional<Portfolio> findById(Long id) {
            return Optional.ofNullable(portfoliosById.get(id));
        }
    }

    private static final class FakeMarketDataService extends MarketDataService {
        private final Map<String, BigDecimal> prices = new HashMap<>();
        private final List<String> requestedTickers = new ArrayList<>();

        FakeMarketDataService() {
            super(null);
        }

        @Override
        public BigDecimal getCurrentPrice(String ticker) {
            requestedTickers.add(ticker);
            if (prices.containsKey(ticker)) {
                return prices.get(ticker);
            }
            String upper = ticker.toUpperCase();
            if (prices.containsKey(upper)) {
                return prices.get(upper);
            }
            String lower = ticker.toLowerCase();
            return prices.get(lower);
        }
    }

    private static final class FakeUserRepository implements UserRepositoryInterface {
        private final Map<Long, BigDecimal> walletBalances = new HashMap<>();
        private Boolean forceAddMoneyResult;
        private Boolean forceRemoveMoneyResult;
        private int addMoneyCalls;
        private int removeMoneyCalls;

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
            return false;
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
            return Optional.ofNullable(walletBalances.get(userId));
        }

        @Override
        public boolean addMoney(Long userId, BigDecimal amount) {
            addMoneyCalls++;
            if (forceAddMoneyResult != null) {
                if (forceAddMoneyResult) {
                    walletBalances.put(userId, walletBalances.get(userId).add(amount));
                }
                return forceAddMoneyResult;
            }
            BigDecimal existing = walletBalances.get(userId);
            if (existing == null) {
                return false;
            }
            walletBalances.put(userId, existing.add(amount));
            return true;
        }

        @Override
        public boolean removeMoney(Long userId, BigDecimal amount) {
            removeMoneyCalls++;
            if (forceRemoveMoneyResult != null) {
                if (forceRemoveMoneyResult) {
                    walletBalances.put(userId, walletBalances.get(userId).subtract(amount));
                }
                return forceRemoveMoneyResult;
            }
            BigDecimal existing = walletBalances.get(userId);
            if (existing == null || existing.compareTo(amount) < 0) {
                return false;
            }
            walletBalances.put(userId, existing.subtract(amount));
            return true;
        }

        @Override
        public void deleteById(Long userId) {
        }
    }

    private static final class FakeWalletTransactionRepository implements WalletTransactionRepositoryInterface {
        private final List<SavedWalletTransaction> savedTransactions = new ArrayList<>();

        @Override
        public WalletTransactionDTO save(Long userId, String transactionType, BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter) {
            SavedWalletTransaction saved = new SavedWalletTransaction(userId, transactionType, amount, balanceBefore, balanceAfter);
            savedTransactions.add(saved);
            return new WalletTransactionDTO((long) savedTransactions.size(), userId, transactionType, amount, balanceBefore, balanceAfter, LocalDateTime.now());
        }

        @Override
        public List<WalletTransactionDTO> findByUserId(Long userId) {
            return List.of();
        }
    }

    private static final class SavedWalletTransaction {
        private final Long userId;
        private final String transactionType;
        private final BigDecimal amount;
        private final BigDecimal before;
        private final BigDecimal after;

        private SavedWalletTransaction(Long userId, String transactionType, BigDecimal amount, BigDecimal before, BigDecimal after) {
            this.userId = userId;
            this.transactionType = transactionType;
            this.amount = amount;
            this.before = before;
            this.after = after;
        }
    }

    @Test
    void findPortfolioOrThrow_returnsPortfolioWhenPresent() {
        TestContext ctx = new TestContext();
        Portfolio portfolio = portfolio(66L, 406L, "Helper");
        ctx.portfolioRepository.portfoliosById.put(66L, portfolio);

        Portfolio result = ReflectionTestUtils.invokeMethod(ctx.service, "findPortfolioOrThrow", 66L);

        assertEquals(66L, result.getId());
    }

    @Test
    void findPortfolioOrThrow_throwsWhenMissing() {
        TestContext ctx = new TestContext();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(ctx.service, "findPortfolioOrThrow", 9999L));

        assertEquals("Portfolio not found with id: 9999", ex.getMessage());
    }
}
