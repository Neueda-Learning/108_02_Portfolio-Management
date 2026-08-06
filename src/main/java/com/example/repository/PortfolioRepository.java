package com.example.repository;

import com.example.model.InvestmentGoal;
import com.example.model.InvestmentHorizon;
import com.example.model.Portfolio;
import com.example.model.RiskLevel;
import com.example.repository.PortfolioItemRepositoryInterface;
import com.example.repository.PortfolioRepositoryInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PortfolioRepository implements PortfolioRepositoryInterface {

    private final JdbcTemplate jdbc;
    private final com.example.repository.PortfolioItemRepositoryInterface portfolioItemRepository;

    public PortfolioRepository(JdbcTemplate jdbc, PortfolioItemRepositoryInterface portfolioItemRepository) {
        this.jdbc = jdbc;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    // ── Row Mapper ──────────────────────────────────────────────────────────────

    private Portfolio mapRow(ResultSet rs, int rowNum) throws SQLException {
        Portfolio p = new Portfolio();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setPortfolioNumber(rs.getLong("portfolio_number"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setCurrency(rs.getString("currency"));
        p.setRiskLevel(toRiskLevel(rs.getString("risk_level")));
        p.setInvestmentGoal(toInvestmentGoal(rs.getString("investment_goal")));
        p.setTargetValue(rs.getBigDecimal("target_value"));
        p.setInvestmentHorizon(toInvestmentHorizon(rs.getString("investment_horizon")));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        return p;
    }

    private Portfolio loadWithItems(Portfolio p) {
        p.setItems(portfolioItemRepository.findByPortfolioId(p.getId()));
        return p;
    }

    // ── Query Methods ────────────────────────────────────────────────────────────

    public List<Portfolio> findAll() {
        List<Portfolio> portfolios = jdbc.query("SELECT * FROM portfolios", this::mapRow);
        portfolios.forEach(this::loadWithItems);
        return portfolios;
    }

    public List<Portfolio> findByUserId(Long userId) {
        List<Portfolio> portfolios = jdbc.query("SELECT * FROM portfolios WHERE user_id = ? ORDER BY portfolio_number", this::mapRow, userId);
        portfolios.forEach(this::loadWithItems);
        return portfolios;
    }

    public Optional<Portfolio> findByUserIdAndPortfolioNumber(Long userId, Long portfolioNumber) {
        List<Portfolio> list = jdbc.query(
                "SELECT * FROM portfolios WHERE user_id = ? AND portfolio_number = ?",
                this::mapRow,
                userId,
                portfolioNumber
        );
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(loadWithItems(list.get(0)));
    }

    public Optional<Portfolio> findById(Long id) {
        List<Portfolio> list = jdbc.query("SELECT * FROM portfolios WHERE id = ?", this::mapRow, id);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(loadWithItems(list.get(0)));
    }

    public Optional<Portfolio> findByName(String name) {
        List<Portfolio> list = jdbc.query("SELECT * FROM portfolios WHERE name = ?", this::mapRow, name);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(loadWithItems(list.get(0)));
    }

    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portfolios WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public boolean existsByName(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portfolios WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }

    public Long getNextPortfolioNumberByUserId(Long userId) {
        Long next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(portfolio_number), 0) + 1 FROM portfolios WHERE user_id = ?",
                Long.class,
                userId
        );
        return next != null ? next : 1L;
    }

    // ── Persistence Methods ──────────────────────────────────────────────────────

    public Portfolio save(Portfolio portfolio) {
        if (portfolio.getId() == null) {
            return insert(portfolio);
        } else {
            return update(portfolio);
        }
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM portfolios WHERE id = ?", id);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────────

    private Portfolio insert(Portfolio portfolio) {
        LocalDateTime now = LocalDateTime.now();
        portfolio.setCreatedAt(now);
        portfolio.setUpdatedAt(now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO portfolios (user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, portfolio.getUserId());
            ps.setLong(2, portfolio.getPortfolioNumber());
            ps.setString(3, portfolio.getName());
            ps.setString(4, portfolio.getDescription());
            ps.setString(5, portfolio.getCurrency());
            ps.setString(6, portfolio.getRiskLevel() != null ? portfolio.getRiskLevel().name() : null);
            ps.setString(7, portfolio.getInvestmentGoal() != null ? portfolio.getInvestmentGoal().name() : null);
            ps.setBigDecimal(8, portfolio.getTargetValue());
            ps.setString(9, portfolio.getInvestmentHorizon() != null ? portfolio.getInvestmentHorizon().name() : null);
            ps.setTimestamp(10, Timestamp.valueOf(portfolio.getCreatedAt()));
            ps.setTimestamp(11, Timestamp.valueOf(portfolio.getUpdatedAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to retrieve generated key for portfolio");
        portfolio.setId(key.longValue());
        return portfolio;
    }

    private Portfolio update(Portfolio portfolio) {
        portfolio.setUpdatedAt(LocalDateTime.now());
        jdbc.update(
                "UPDATE portfolios SET name = ?, description = ?, currency = ?, risk_level = ?, investment_goal = ?, target_value = ?, investment_horizon = ?, updated_at = ? WHERE id = ?",
                portfolio.getName(), portfolio.getDescription(), portfolio.getCurrency(),
                portfolio.getRiskLevel() != null ? portfolio.getRiskLevel().name() : null,
                portfolio.getInvestmentGoal() != null ? portfolio.getInvestmentGoal().name() : null,
                portfolio.getTargetValue(),
                portfolio.getInvestmentHorizon() != null ? portfolio.getInvestmentHorizon().name() : null,
                Timestamp.valueOf(portfolio.getUpdatedAt()), portfolio.getId());
        return portfolio;
    }

    private RiskLevel toRiskLevel(String value) {
        return value == null ? null : RiskLevel.valueOf(value);
    }

    private InvestmentGoal toInvestmentGoal(String value) {
        return value == null ? null : InvestmentGoal.valueOf(value);
    }

    private InvestmentHorizon toInvestmentHorizon(String value) {
        return value == null ? null : InvestmentHorizon.valueOf(value);
    }
}

