package com.example.repository;

import com.example.dto.WalletTransactionDTO;
import com.example.repository.WalletTransactionRepositoryInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class WalletTransactionRepository implements WalletTransactionRepositoryInterface {

    private final JdbcTemplate jdbc;

    public WalletTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private WalletTransactionDTO mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new WalletTransactionDTO(
                rs.getLong("transaction_id"),
                rs.getLong("user_id"),
                rs.getString("transaction_type"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_before"),
                rs.getBigDecimal("balance_after"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }

    @Override
    public WalletTransactionDTO save(Long userId, String transactionType, BigDecimal amount,
                                     BigDecimal balanceBefore, BigDecimal balanceAfter) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO wallet_transactions (user_id, transaction_type, amount, balance_before, balance_after, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, transactionType);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, balanceBefore);
            ps.setBigDecimal(5, balanceAfter);
            ps.setTimestamp(6, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long transactionId = key != null ? key.longValue() : null;
        return new WalletTransactionDTO(transactionId, userId, transactionType, amount, balanceBefore, balanceAfter, now);
    }

    @Override
    public List<WalletTransactionDTO> findByUserId(Long userId) {
        return jdbc.query(
                "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC, transaction_id DESC",
                this::mapRow,
                userId
        );
    }
}


