package com.example.repository;

import com.example.model.User;
import com.example.repository.UserRepositoryInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository implements UserRepositoryInterface {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Row Mapper ──────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        BigDecimal walletBalance = rs.getBigDecimal("wallet_balance");
        user.setWalletBalance(walletBalance != null ? walletBalance : BigDecimal.ZERO);
        return user;
    }

    // ── Query Methods ────────────────────────────────────────────────────────────

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users", this::mapRow);
    }

    public Optional<User> findById(Long userId) {
        List<User> users = jdbc.query("SELECT * FROM users WHERE user_id = ?", this::mapRow, userId);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbc.query("SELECT * FROM users WHERE username = ?", this::mapRow, username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public boolean existsById(Long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ?", Integer.class, userId);
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    // ── Persistence Methods ──────────────────────────────────────────────────────

    public User save(User user) {
        if (user.getUserId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    public Optional<BigDecimal> getWalletBalance(Long userId) {
        List<BigDecimal> balances = jdbc.query(
                "SELECT wallet_balance FROM users WHERE user_id = ?",
                (rs, rowNum) -> {
                    BigDecimal walletBalance = rs.getBigDecimal("wallet_balance");
                    return walletBalance != null ? walletBalance : BigDecimal.ZERO;
                },
                userId
        );
        return balances.isEmpty() ? Optional.empty() : Optional.of(balances.get(0));
    }

    public boolean addMoney(Long userId, BigDecimal amount) {
        int updated = jdbc.update(
                "UPDATE users SET wallet_balance = wallet_balance + ? WHERE user_id = ?",
                amount, userId
        );
        return updated > 0;
    }

    public boolean removeMoney(Long userId, BigDecimal amount) {
        int updated = jdbc.update(
                "UPDATE users SET wallet_balance = wallet_balance - ? WHERE user_id = ? AND wallet_balance >= ?",
                amount, userId, amount
        );
        return updated > 0;
    }

    public void deleteById(Long userId) {
        jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────────

    private User insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (username, wallet_balance) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setBigDecimal(2, user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to retrieve generated key for user");
        user.setUserId(key.longValue());
        return user;
    }

    private User update(User user) {
        jdbc.update(
                "UPDATE users SET username = ?, wallet_balance = ? WHERE user_id = ?",
                user.getUsername(),
                user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO,
                user.getUserId());
        return user;
    }
}


