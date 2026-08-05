package com.example.controller;

import com.example.dto.WalletBalanceDTO;
import com.example.dto.WalletTransactionDTO;
import com.example.dto.WalletTransactionRequest;
import com.example.exception.InsufficientWalletBalanceException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.User;
import com.example.repository.UserRepositoryInterface;
import com.example.service.WalletServiceInterface;
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
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserRepositoryInterface userRepository;

	@MockBean
	private WalletServiceInterface walletService;

	@Test
	void getAllUsers_returns200_withAllUsers() throws Exception {
		when(userRepository.findAll()).thenReturn(List.of(
				buildUser(1L, "alice", "100.00"),
				buildUser(2L, "bob", "250.50")
		));

		mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].userId", is(1)))
				.andExpect(jsonPath("$[0].username", is("alice")))
				.andExpect(jsonPath("$[1].walletBalance", is(250.50)));
	}

	@Test
	void getAllUsers_returns200_withEmptyList() throws Exception {
		when(userRepository.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void getUserById_returns200_whenUserExists() throws Exception {
		when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice", "100.00")));

		mockMvc.perform(get("/api/users/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(1)))
				.andExpect(jsonPath("$.username", is("alice")))
				.andExpect(jsonPath("$.walletBalance", is(100.00)));
	}

	@Test
	void getUserById_returns404_whenUserMissing() throws Exception {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/users/99").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void getUserByUsername_returns200_whenUserExists() throws Exception {
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser(1L, "alice", "100.00")));

		mockMvc.perform(get("/api/users/username/alice").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(1)))
				.andExpect(jsonPath("$.username", is("alice")));
	}

	@Test
	void getUserByUsername_returns404_whenUserMissing() throws Exception {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/users/username/ghost").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void getWalletBalance_returns200_withBalance() throws Exception {
		when(walletService.getWalletBalance(1L)).thenReturn(new WalletBalanceDTO(1L, new BigDecimal("150.75")));

		mockMvc.perform(get("/api/users/1/wallet").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(1)))
				.andExpect(jsonPath("$.balance", is(150.75)));
	}

	@Test
	void addMoney_returns200_withUpdatedBalance() throws Exception {
		WalletTransactionRequest request = new WalletTransactionRequest(new BigDecimal("25.00"));
		when(walletService.addMoney(eq(1L), any(WalletTransactionRequest.class)))
				.thenReturn(new WalletBalanceDTO(1L, new BigDecimal("175.75")));

		mockMvc.perform(post("/api/users/1/wallet/add")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(1)))
				.andExpect(jsonPath("$.balance", is(175.75)));
	}

	@Test
	void removeMoney_returns200_withUpdatedBalance() throws Exception {
		WalletTransactionRequest request = new WalletTransactionRequest(new BigDecimal("25.00"));
		when(walletService.removeMoney(eq(1L), any(WalletTransactionRequest.class)))
				.thenReturn(new WalletBalanceDTO(1L, new BigDecimal("125.75")));

		mockMvc.perform(post("/api/users/1/wallet/remove")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(1)))
				.andExpect(jsonPath("$.balance", is(125.75)));
	}

	@Test
	void getWalletTransactions_returns200_withHistory() throws Exception {
		when(walletService.getTransactionHistory(1L)).thenReturn(List.of(
				new WalletTransactionDTO(10L, 1L, "DEPOSIT", new BigDecimal("25.00"), new BigDecimal("100.00"), new BigDecimal("125.00"), LocalDateTime.of(2026, 8, 5, 10, 0)),
				new WalletTransactionDTO(11L, 1L, "WITHDRAWAL", new BigDecimal("10.00"), new BigDecimal("125.00"), new BigDecimal("115.00"), LocalDateTime.of(2026, 8, 5, 11, 0))
		));

		mockMvc.perform(get("/api/users/1/wallet/transactions").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].transactionId", is(10)))
				.andExpect(jsonPath("$[0].transactionType", is("DEPOSIT")))
				.andExpect(jsonPath("$[1].transactionType", is("WITHDRAWAL")));
	}

	@Test
	void createUser_returns200_withSavedUser() throws Exception {
		User request = buildUser(null, "charlie", "50.00");
		User saved = buildUser(3L, "charlie", "50.00");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(3)))
				.andExpect(jsonPath("$.username", is("charlie")))
				.andExpect(jsonPath("$.walletBalance", is(50.00)));
	}

	@Test
	void deleteUser_returns204_whenUserExists() throws Exception {
		when(userRepository.existsById(1L)).thenReturn(true);
		doNothing().when(userRepository).deleteById(1L);

		mockMvc.perform(delete("/api/users/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void deleteUser_returns404_whenUserMissing() throws Exception {
		when(userRepository.existsById(99L)).thenReturn(false);

		mockMvc.perform(delete("/api/users/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void addMoney_returns400_whenValidationFails() throws Exception {
		mockMvc.perform(post("/api/users/1/wallet/add")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":0}")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Validation failed")))
				.andExpect(jsonPath("$.fieldErrors.amount", is("Amount must be greater than 0")));
	}

	@Test
	void removeMoney_returns400_whenValidationFails() throws Exception {
		mockMvc.perform(post("/api/users/1/wallet/remove")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":0}")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Validation failed")))
				.andExpect(jsonPath("$.fieldErrors.amount", is("Amount must be greater than 0")));
	}

	@Test
	void addMoney_returns400_forMalformedJson() throws Exception {
		mockMvc.perform(post("/api/users/1/wallet/add")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed JSON request body")));
	}

	@Test
	void removeMoney_returns409_whenServiceThrowsInsufficientBalance() throws Exception {
		WalletTransactionRequest request = new WalletTransactionRequest(new BigDecimal("500.00"));
		when(walletService.removeMoney(eq(1L), any(WalletTransactionRequest.class)))
				.thenThrow(new InsufficientWalletBalanceException(1L, new BigDecimal("500.00")));

		mockMvc.perform(post("/api/users/1/wallet/remove")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", is("Insufficient wallet balance for user id 1 to withdraw amount 500.00")));
	}

	@Test
	void walletEndpoints_return404_whenServiceThrowsResourceNotFound() throws Exception {
		when(walletService.getWalletBalance(404L)).thenThrow(new ResourceNotFoundException("User not found"));
		when(walletService.getTransactionHistory(404L)).thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(get("/api/users/404/wallet").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User not found")));

		mockMvc.perform(get("/api/users/404/wallet/transactions").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User not found")));
	}

	private User buildUser(Long userId, String username, String walletBalance) {
		User user = new User();
		user.setUserId(userId);
		user.setUsername(username);
		user.setWalletBalance(new BigDecimal(walletBalance));
		return user;
	}
}
