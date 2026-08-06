package com.example.repository;

import com.example.model.AssetType;
import com.example.model.Portfolio;
import com.example.model.PortfolioItem;
import com.example.repository.PortfolioItemRepositoryInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PortfolioItemRepository implements PortfolioItemRepositoryInterface {

    private final JdbcTemplate jdbc;

    public PortfolioItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Row Mapper ──────────────────────────────────────────────────────────────

    private PortfolioItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        PortfolioItem item = new PortfolioItem();
        item.setId(rs.getLong("id"));

        Portfolio portfolio = new Portfolio();
        portfolio.setId(rs.getLong("portfolio_id"));
        item.setPortfolio(portfolio);

        item.setAssetType(AssetType.valueOf(rs.getString("asset_type")));
        item.setSymbol(rs.getString("symbol"));
        item.setName(rs.getString("name"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        item.setCurrentPrice(rs.getBigDecimal("current_price"));
        item.setPurchaseDate(rs.getTimestamp("purchase_date").toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        item.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        item.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        item.setNotes(rs.getString("notes"));
        return item;
    }

    // ── Query Methods ────────────────────────────────────────────────────────────

    public List<PortfolioItem> findByPortfolioId(Long portfolioId) {
        return jdbc.query("SELECT * FROM portfolio_items WHERE portfolio_id = ?", this::mapRow, portfolioId);
    }

    public List<PortfolioItem> findByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType) {
        return jdbc.query(
                "SELECT * FROM portfolio_items WHERE portfolio_id = ? AND asset_type = ?",
                this::mapRow, portfolioId, assetType.name());
    }

    public Optional<PortfolioItem> findByPortfolioIdAndSymbol(Long portfolioId, String symbol) {
        List<PortfolioItem> items = jdbc.query(
                "SELECT * FROM portfolio_items WHERE portfolio_id = ? AND symbol = ?",
                this::mapRow, portfolioId, symbol);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public List<PortfolioItem> findBySymbol(String symbol) {
        return jdbc.query("SELECT * FROM portfolio_items WHERE symbol = ?", this::mapRow, symbol);
    }

    public Optional<PortfolioItem> findById(Long id) {
        List<PortfolioItem> items = jdbc.query(
                "SELECT * FROM portfolio_items WHERE id = ?", this::mapRow, id);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portfolio_items WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    // ── Persistence Methods ──────────────────────────────────────────────────────

    public PortfolioItem save(PortfolioItem item) {
        if (item.getId() == null) {
            return insert(item);
        } else {
            return update(item);
        }
    }

    public List<PortfolioItem> saveAll(List<PortfolioItem> items) {
        items.forEach(this::save);
        return items;
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM portfolio_items WHERE id = ?", id);
    }

    public void deleteByPortfolioId(Long portfolioId) {
        jdbc.update("DELETE FROM portfolio_items WHERE portfolio_id = ?", portfolioId);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────────

    private PortfolioItem insert(PortfolioItem item) {
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        if (item.getPurchaseDate() == null) {
            item.setPurchaseDate(now);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO portfolio_items " +
                    "(portfolio_id, asset_type, symbol, name, quantity, purchase_price, " +
                    " current_price, purchase_date, created_at, updated_at, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getPortfolio().getId());
            ps.setString(2, item.getAssetType().name());
            ps.setString(3, item.getSymbol());
            ps.setString(4, item.getName());
            ps.setBigDecimal(5, item.getQuantity());
            ps.setBigDecimal(6, item.getPurchasePrice());
            ps.setBigDecimal(7, item.getCurrentPrice());
            ps.setTimestamp(8, Timestamp.valueOf(item.getPurchaseDate()));
            ps.setTimestamp(9, Timestamp.valueOf(item.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(item.getUpdatedAt()));
            ps.setString(11, item.getNotes());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to retrieve generated key for portfolio_item");
        item.setId(key.longValue());
        return item;
    }

    private PortfolioItem update(PortfolioItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        jdbc.update(
                "UPDATE portfolio_items SET asset_type = ?, symbol = ?, name = ?, quantity = ?, " +
                "purchase_price = ?, current_price = ?, purchase_date = ?, updated_at = ?, notes = ? " +
                "WHERE id = ?",
                item.getAssetType().name(), item.getSymbol(), item.getName(),
                item.getQuantity(), item.getPurchasePrice(), item.getCurrentPrice(),
                Timestamp.valueOf(item.getPurchaseDate()),
                Timestamp.valueOf(item.getUpdatedAt()), item.getNotes(), item.getId());
        return item;
    }
}

