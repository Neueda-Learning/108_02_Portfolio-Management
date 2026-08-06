package com.example.service;

import com.example.dto.WalletBalanceDTO;
import com.example.dto.WalletTransactionDTO;
import com.example.dto.WalletTransactionRequest;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.UserNotFoundException;
import com.example.repository.UserRepositoryInterface;
import com.example.repository.WalletTransactionRepositoryInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	@Mock
	private UserRepositoryInterface userRepository;

	@Mock
	private WalletTransactionRepositoryInterface walletTransactionRepository;

	@InjectMocks
	private WalletService walletService;

	@Test
	void getWalletBalance_returnsBalanceForExistingUser() {
		Long userId = 1L;
		BigDecimal balance = new BigDecimal("1500.25");
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(balance));

		WalletBalanceDTO result = walletService.getWalletBalance(userId);

		assertAll(
				() -> assertEquals(userId, result.userId()),
				() -> assertEquals(balance, result.balance())
		);
		verify(userRepository).getWalletBalance(userId);
		verifyNoInteractions(walletTransactionRepository);
	}

	@Test
	void getWalletBalance_throwsWhenUserDoesNotExist() {
		Long userId = 99L;
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.empty());

		UserNotFoundException exception = assertThrows(UserNotFoundException.class,
				() -> walletService.getWalletBalance(userId));

		assertEquals("User not found with id: 99", exception.getMessage());
		verify(userRepository).getWalletBalance(userId);
		verifyNoInteractions(walletTransactionRepository);
	}

	@Test
	void addMoney_updatesBalanceAndStoresDepositTransaction() {
		Long userId = 1L;
		BigDecimal before = new BigDecimal("100.00");
		BigDecimal amount = new BigDecimal("25.50");
		BigDecimal after = new BigDecimal("125.50");
		WalletTransactionRequest request = new WalletTransactionRequest(amount);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(before));
		when(userRepository.addMoney(userId, amount)).thenReturn(true);

		WalletBalanceDTO result = walletService.addMoney(userId, request);

		assertAll(
				() -> assertEquals(userId, result.userId()),
				() -> assertEquals(after, result.balance())
		);
		verify(userRepository).getWalletBalance(userId);
		verify(userRepository).addMoney(userId, amount);
		verify(walletTransactionRepository).save(userId, "DEPOSIT", amount, before, after);
	}

	@Test
	void addMoney_throwsWhenRepositoryCannotUpdateWallet() {
		Long userId = 1L;
		BigDecimal before = new BigDecimal("100.00");
		BigDecimal amount = new BigDecimal("25.50");
		WalletTransactionRequest request = new WalletTransactionRequest(amount);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(before));
		when(userRepository.addMoney(userId, amount)).thenReturn(false);

		UserNotFoundException exception = assertThrows(UserNotFoundException.class,
				() -> walletService.addMoney(userId, request));

		assertEquals("User not found with id: 1", exception.getMessage());
		verify(userRepository).getWalletBalance(userId);
		verify(userRepository).addMoney(userId, amount);
		verify(walletTransactionRepository, never()).save(userId, "DEPOSIT", amount, before, before.add(amount));
	}

	@Test
	void removeMoney_updatesBalanceAndStoresWithdrawTransaction() {
		Long userId = 1L;
		BigDecimal before = new BigDecimal("100.00");
		BigDecimal amount = new BigDecimal("40.00");
		BigDecimal after = new BigDecimal("60.00");
		WalletTransactionRequest request = new WalletTransactionRequest(amount);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(before));
		when(userRepository.removeMoney(userId, amount)).thenReturn(true);

		WalletBalanceDTO result = walletService.removeMoney(userId, request);

		assertAll(
				() -> assertEquals(userId, result.userId()),
				() -> assertEquals(after, result.balance())
		);
		verify(userRepository).getWalletBalance(userId);
		verify(userRepository).removeMoney(userId, amount);
		verify(walletTransactionRepository).save(userId, "WITHDRAW", amount, before, after);
	}

	@Test
	void removeMoney_throwsWhenRequestedAmountExceedsCurrentBalance() {
		Long userId = 1L;
		BigDecimal before = new BigDecimal("10.00");
		BigDecimal amount = new BigDecimal("15.00");
		WalletTransactionRequest request = new WalletTransactionRequest(amount);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(before));

		InsufficientWalletBalanceException exception = assertThrows(
				InsufficientWalletBalanceException.class,
				() -> walletService.removeMoney(userId, request)
		);

		assertEquals("Insufficient wallet balance for user id 1 to withdraw amount 15.00",
				exception.getMessage());
		verify(userRepository).getWalletBalance(userId);
		verify(userRepository, never()).removeMoney(userId, amount);
		verifyNoInteractions(walletTransactionRepository);
	}

	@Test
	void removeMoney_throwsWhenRepositoryRefusesUpdate() {
		Long userId = 1L;
		BigDecimal before = new BigDecimal("100.00");
		BigDecimal amount = new BigDecimal("40.00");
		WalletTransactionRequest request = new WalletTransactionRequest(amount);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(before));
		when(userRepository.removeMoney(userId, amount)).thenReturn(false);

		InsufficientWalletBalanceException exception = assertThrows(
				InsufficientWalletBalanceException.class,
				() -> walletService.removeMoney(userId, request)
		);

		assertEquals("Insufficient wallet balance for user id 1 to withdraw amount 40.00",
				exception.getMessage());
		verify(userRepository).getWalletBalance(userId);
		verify(userRepository).removeMoney(userId, amount);
		verify(walletTransactionRepository, never()).save(userId, "WITHDRAW", amount, before, before.subtract(amount));
	}

	@Test
	void getTransactionHistory_returnsTransactionsForExistingUser() {
		Long userId = 1L;
		BigDecimal balance = new BigDecimal("100.00");
		WalletTransactionDTO transaction = new WalletTransactionDTO(
				11L,
				userId,
				"DEPOSIT",
				new BigDecimal("25.00"),
				new BigDecimal("75.00"),
				balance,
				LocalDateTime.of(2026, 8, 4, 10, 15)
		);
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.of(balance));
		when(walletTransactionRepository.findByUserId(userId)).thenReturn(List.of(transaction));

		List<WalletTransactionDTO> result = walletService.getTransactionHistory(userId);

		assertSame(transaction, result.get(0));
		verify(userRepository).getWalletBalance(userId);
		verify(walletTransactionRepository).findByUserId(userId);
	}

	@Test
	void getTransactionHistory_throwsWhenUserDoesNotExist() {
		Long userId = 1L;
		when(userRepository.getWalletBalance(userId)).thenReturn(Optional.empty());

		UserNotFoundException exception = assertThrows(UserNotFoundException.class,
				() -> walletService.getTransactionHistory(userId));

		assertEquals("User not found with id: 1", exception.getMessage());
		verify(userRepository).getWalletBalance(userId);
		verifyNoInteractions(walletTransactionRepository);
	}
}
